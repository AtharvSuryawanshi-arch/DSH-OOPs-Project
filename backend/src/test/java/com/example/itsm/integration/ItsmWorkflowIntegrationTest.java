package com.example.itsm.integration;

import com.example.itsm.catalog.BusinessCriticality;
import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.CategoryRepository;
import com.example.itsm.catalog.ItService;
import com.example.itsm.catalog.ItServiceRepository;
import com.example.itsm.incident.Priority;
import com.example.itsm.settings.SettingsService;
import com.example.itsm.sla.SlaPolicy;
import com.example.itsm.sla.SlaPolicyRepository;
import com.example.itsm.user.Role;
import com.example.itsm.user.UserFactory;
import com.example.itsm.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "app.seed.enabled=false",
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs384-123456",
                "app.sla.monitor-initial-delay-ms=3600000",
                "app.sla.monitor-interval-ms=3600000"
        }
)
@AutoConfigureMockMvc
@Testcontainers
class ItsmWorkflowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("itsm_test")
            .withUsername("itsm")
            .withPassword("itsm");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItServiceRepository serviceRepository;

    @Autowired
    private SlaPolicyRepository slaPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SettingsService settingsService;

    @BeforeEach
    void seedMinimalTenant() {
        if (userRepository.count() > 0) {
            return;
        }

        Category category = categoryRepository.save(new Category("APPLICATION", "Application", "Application issues"));
        serviceRepository.save(new ItService(
                "PAYMENT",
                "Payment Gateway",
                "Payment services",
                BusinessCriticality.CRITICAL
        ));
        slaPolicyRepository.save(new SlaPolicy("P1 global", Priority.P1, null, 5, 30, 80));

        userRepository.save(userFactory.create(
                Role.ADMIN, "Admin User", "admin@test.local",
                passwordEncoder.encode("Admin@123"), null, null, "IT Manager", null, null, null
        ));
        userRepository.save(userFactory.create(
                Role.EMPLOYEE, "Employee User", "employee@test.local",
                passwordEncoder.encode("Employee@123"), null, "Finance", null, null, null, null
        ));
        userRepository.save(userFactory.create(
                Role.TECHNICIAN, "Tech User", "tech@test.local",
                passwordEncoder.encode("Tech@123"), null, null, null, 5, 8, Set.of(category)
        ));

        settingsService.set(SettingsService.ASSIGNMENT_STRATEGY, "SKILL_BASED");
        settingsService.set(SettingsService.AUTO_ASSIGN, "true");
    }

    @Test
    void employeeReportFlowsThroughResolutionVerificationAndClosure() throws Exception {
        String employeeToken = login("employee@test.local", "Employee@123");
        String techToken = login("tech@test.local", "Tech@123");
        String adminToken = login("admin@test.local", "Admin@123");

        MvcResult createdResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Payment application unavailable",
                                "description", "The payment application is down and checkout is failing for all customers.",
                                "serviceId", 1,
                                "impact", "HIGH",
                                "urgency", "HIGH",
                                "affectedUsers", 500
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createdResult.getResponse().getContentAsString());
        long incidentId = created.get("id").asLong();
        assertThat(created.get("priority").asText()).isEqualTo("P1");
        assertThat(created.get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(created.get("technicianName").asText()).isEqualTo("Tech User");
        assertThat(created.get("categoryName").asText()).isEqualTo("Application");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\"Too early\"}"))
                .andExpect(status().isConflict());

        transition("/api/incidents/" + incidentId + "/acknowledge", techToken)
                .andExpect(status().isOk());
        transition("/api/incidents/" + incidentId + "/start", techToken)
                .andExpect(status().isOk());

        MvcResult resolved = mockMvc.perform(post("/api/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\"Rolled back the deployment and restored the payment service.\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(resolved.getResponse().getContentAsString()).get("status").asText())
                .isEqualTo("RESOLVED");

        transition("/api/incidents/" + incidentId + "/verify", employeeToken)
                .andExpect(status().isOk());
        transition("/api/incidents/" + incidentId + "/close", adminToken)
                .andExpect(status().isOk());

        MvcResult feedback = mockMvc.perform(post("/api/incidents/" + incidentId + "/feedback")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Great turnaround\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode feedbackBody = objectMapper.readTree(feedback.getResponse().getContentAsString());
        assertThat(feedbackBody.get("status").asText()).isEqualTo("CLOSED");
        assertThat(feedbackBody.get("feedbackRating").asInt()).isEqualTo(5);
        assertThat(feedbackBody.get("slaStatus").asText()).isEqualTo("MET");

        MvcResult detail = mockMvc.perform(get("/api/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detailBody = objectMapper.readTree(detail.getResponse().getContentAsString());
        assertThat(detailBody.get("version").asLong()).isGreaterThan(0);
        assertThat(detailBody.get("affectedUsers").asInt()).isEqualTo(500);
        assertThat(detailBody.get("comments").size()).isZero();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions transition(String path, String token)
            throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token));
    }
}
