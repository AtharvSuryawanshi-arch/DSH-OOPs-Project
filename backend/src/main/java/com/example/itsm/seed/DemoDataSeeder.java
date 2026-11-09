package com.example.itsm.seed;

import com.example.itsm.audit.AuditAction;
import com.example.itsm.audit.AuditService;
import com.example.itsm.catalog.BusinessCriticality;
import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.CategoryRepository;
import com.example.itsm.catalog.ItService;
import com.example.itsm.catalog.ItServiceRepository;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentComment;
import com.example.itsm.incident.IncidentCommentRepository;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.incident.Impact;
import com.example.itsm.incident.Priority;
import com.example.itsm.incident.Urgency;
import com.example.itsm.incident.priority.PriorityContext;
import com.example.itsm.incident.priority.PriorityResult;
import com.example.itsm.incident.priority.PriorityStrategy;
import com.example.itsm.notification.Notification;
import com.example.itsm.notification.NotificationChannel;
import com.example.itsm.notification.NotificationRepository;
import com.example.itsm.notification.NotificationType;
import com.example.itsm.settings.SettingsService;
import com.example.itsm.sla.SlaPolicy;
import com.example.itsm.sla.SlaPolicyRepository;
import com.example.itsm.user.Administrator;
import com.example.itsm.user.Employee;
import com.example.itsm.user.Role;
import com.example.itsm.user.Technician;
import com.example.itsm.user.User;
import com.example.itsm.user.UserFactory;
import com.example.itsm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final CategoryRepository categoryRepository;
    private final ItServiceRepository serviceRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentCommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final PriorityStrategy priorityStrategy;
    private final SettingsService settingsService;
    private final Clock clock;

    private Map<Priority, SlaPolicy> globalPolicies;
    private SlaPolicy paymentP1Policy;

    public DemoDataSeeder(
            UserRepository userRepository,
            UserFactory userFactory,
            CategoryRepository categoryRepository,
            ItServiceRepository serviceRepository,
            SlaPolicyRepository slaPolicyRepository,
            IncidentRepository incidentRepository,
            IncidentCommentRepository commentRepository,
            NotificationRepository notificationRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            PriorityStrategy priorityStrategy,
            SettingsService settingsService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.priorityStrategy = priorityStrategy;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already present; skipping seeder.");
            return;
        }
        log.info("Seeding I-ITSM demo data...");

        Map<String, Category> categories = seedCategories();
        Map<String, ItService> services = seedServices();
        seedSlaPolicies(services);
        settingsService.set(SettingsService.ASSIGNMENT_STRATEGY, "SKILL_BASED");
        settingsService.set(SettingsService.AUTO_ASSIGN, "true");

        Administrator admin = (Administrator) userFactory.create(
                Role.ADMIN, "Aarav Mehta", "admin@itsm.local",
                passwordEncoder.encode("Admin@123"), "+91 90000 10001",
                null, "IT Service Manager", null, null, null
        );
        userRepository.save(admin);

        Technician priya = tech("Priya Nair", "priya@itsm.local", "Tech@123", 7, 8,
                categories, "APPLICATION", "DATABASE", "CLOUD");
        Technician rohan = tech("Rohan Verma", "rohan@itsm.local", "Tech@123", 5, 8,
                categories, "NETWORK", "SECURITY", "AUTHENTICATION");
        Technician sara = tech("Sara Khan", "sara@itsm.local", "Tech@123", 3, 6,
                categories, "HARDWARE", "SOFTWARE", "STORAGE");
        Technician dev = tech("Dev Patel", "dev@itsm.local", "Tech@123", 2, 6,
                categories, "APPLICATION", "AUTHENTICATION", "NETWORK");
        List<Technician> technicians = List.of(priya, rohan, sara, dev);

        Employee neha = employee("Neha Sharma", "employee@itsm.local", "Employee@123", "Finance", "+91 90000 20001");
        Employee vikram = employee("Vikram Rao", "vikram@itsm.local", "Employee@123", "Operations", "+91 90000 20002");
        userRepository.saveAll(List.of(neha, vikram));

        Instant now = clock.instant();

        Incident paymentClosed = seedIncident(
                "Payment gateway returning 503 for card transactions",
                "Customers cannot complete card payments. The gateway reports HTTP 503 from the payment service. The issue began after the evening release.",
                neha, categories.get("APPLICATION"), services.get("PAYMENT_GATEWAY"),
                Impact.EXTREME, Urgency.CRITICAL, 800
        ).technician(priya).status(IncidentStatus.CLOSED)
                .createdMinutesAgo(7200).ackMinutesAfter(1).resolveMinutesAfter(10).closeMinutesAfter(2880)
                .resolutionNotes("Rolled back the release, drained unhealthy gateway pods, and confirmed successful card authorization with the monitoring probe.")
                .closeNotes("Verified by the reporter and closed after 48 hours of stable monitoring.")
                .build();

        Incident ssoInProgress = seedIncident(
                "Unable to sign in to SSO after password reset",
                "Several users receive an access-denied loop after resetting their campus password. The identity provider logs show an invalid SSO session token.",
                vikram, categories.get("AUTHENTICATION"), services.get("IDENTITY_SSO"),
                Impact.MEDIUM, Urgency.HIGH, 45
        ).technician(rohan).status(IncidentStatus.IN_PROGRESS)
                .createdMinutesAgo(90).ackMinutesAfter(5)
                .build();

        Incident vpnAcknowledged = seedIncident(
                "VPN disconnects every few minutes from home network",
                "The VPN client drops the connection roughly every five minutes when working from home. Reconnecting works temporarily but interrupts file transfers.",
                neha, categories.get("NETWORK"), services.get("VPN"),
                Impact.MEDIUM, Urgency.MEDIUM, 12
        ).technician(rohan).status(IncidentStatus.ACKNOWLEDGED)
                .createdMinutesAgo(50).ackMinutesAfter(10)
                .build();

        Incident databaseResolved = seedIncident(
                "Database replication lag causing stale reporting",
                "The reporting replica is 40 minutes behind the primary database. Finance dashboards show stale figures and month-end reports are delayed.",
                vikram, categories.get("DATABASE"), services.get("DATABASE_CLUSTER"),
                Impact.HIGH, Urgency.HIGH, 300
        ).technician(priya).status(IncidentStatus.RESOLVED)
                .createdMinutesAgo(360).ackMinutesAfter(4).resolveMinutesAfter(50)
                .resolutionNotes("Recreated the replica, increased WAL retention, and replayed the backlog. Lag is now below one second.")
                .build();

        Incident laptopAssigned = seedIncident(
                "Laptop will not boot after firmware update",
                "The laptop shows a black screen after the latest firmware update. The user has an important presentation this afternoon and needs a working device.",
                neha, categories.get("HARDWARE"), services.get("END_USER_COMPUTING"),
                Impact.LOW, Urgency.MEDIUM, 1
        ).technician(sara).status(IncidentStatus.ASSIGNED)
                .createdMinutesAgo(120)
                .build();

        Incident phishingClassified = seedIncident(
                "Phishing email targeting finance team",
                "The finance team received an email pretending to be the CFO and asking for an urgent bank transfer. The message includes a suspicious link and an attachment.",
                neha, categories.get("SECURITY"), services.get("EMAIL_COLLABORATION"),
                Impact.HIGH, Urgency.HIGH, 120
        ).status(IncidentStatus.CLASSIFIED)
                .createdMinutesAgo(20)
                .build();

        Incident hrPortalInProgress = seedIncident(
                "HR portal shows a blank dashboard for all users",
                "After the morning deployment, the HR portal dashboard is empty for every employee. The underlying APIs respond successfully, so the issue appears to be in the frontend bundle.",
                vikram, categories.get("APPLICATION"), services.get("HR_PORTAL"),
                Impact.HIGH, Urgency.HIGH, 75
        ).technician(priya).status(IncidentStatus.IN_PROGRESS)
                .createdMinutesAgo(60).ackMinutesAfter(10)
                .build();

        Incident cloudStorageAssigned = seedIncident(
                "Cloud storage bucket permissions changed unexpectedly",
                "A production object storage bucket is returning access-denied errors. The permissions appear to have changed after an infrastructure automation run.",
                neha, categories.get("CLOUD"), services.get("CLOUD_STORAGE"),
                Impact.MEDIUM, Urgency.HIGH, 30
        ).technician(dev).status(IncidentStatus.ASSIGNED)
                .createdMinutesAgo(40)
                .build();

        Incident paymentDuplicate = seedIncident(
                "Payment transactions failing for some checkout attempts",
                "Some checkout attempts fail with a timeout while others succeed. This looks related to the ongoing payment gateway instability reported by the finance team.",
                vikram, categories.get("APPLICATION"), services.get("PAYMENT_GATEWAY"),
                Impact.HIGH, Urgency.CRITICAL, 60
        ).status(IncidentStatus.CLASSIFIED)
                .createdMinutesAgo(10)
                .build();

        Incident paymentOpen = seedIncident(
                "Payment application unavailable after deployment",
                "The payment application is unavailable for all users following the 14:00 deployment. Checkout requests time out and transaction volume has dropped to zero.",
                neha, categories.get("APPLICATION"), services.get("PAYMENT_GATEWAY"),
                Impact.EXTREME, Urgency.CRITICAL, 500
        ).technician(priya).status(IncidentStatus.IN_PROGRESS)
                .createdMinutesAgo(120).ackMinutesAfter(3)
                .build();

        Incident emailVerified = seedIncident(
                "Email delivery delayed for external recipients",
                "Outbound email to external domains is delayed by 30 to 60 minutes. Internal mail is unaffected. The mail queue has a growing backlog.",
                vikram, categories.get("NETWORK"), services.get("EMAIL_COLLABORATION"),
                Impact.MEDIUM, Urgency.MEDIUM, 40
        ).technician(rohan).status(IncidentStatus.VERIFIED)
                .createdMinutesAgo(1440).ackMinutesAfter(8).resolveMinutesAfter(300)
                .resolutionNotes("Cleared the outbound queue, renewed the relay certificate, and confirmed delivery to external test recipients.")
                .build();

        Incident storageClosed = seedIncident(
                "Shared drive quota warnings across design team",
                "Designers are receiving quota warnings on the shared project drive. Large assets cannot be uploaded, which is blocking the campaign review.",
                neha, categories.get("STORAGE"), services.get("CLOUD_STORAGE"),
                Impact.LOW, Urgency.HIGH, 25
        ).technician(sara).status(IncidentStatus.CLOSED)
                .createdMinutesAgo(10080).ackMinutesAfter(15).resolveMinutesAfter(200).closeMinutesAfter(300)
                .resolutionNotes("Extended the team quota and archived old assets according to the retention policy.")
                .closeNotes("Reporter confirmed the shared drive is working normally.")
                .build();

        Incident authAtRisk = seedIncident(
                "Authentication latency spikes during peak login",
                "Single sign-on latency rises above eight seconds during the morning peak. Some users time out before the login flow completes.",
                vikram, categories.get("AUTHENTICATION"), services.get("IDENTITY_SSO"),
                Impact.MEDIUM, Urgency.HIGH, 45
        ).technician(rohan).status(IncidentStatus.IN_PROGRESS)
                .createdMinutesAgo(110).ackMinutesAfter(5)
                .build();

        // Historical feedback
        paymentClosed.recordFeedback(5, "Fast rollback and clear communication throughout. Excellent response.");
        auditService.recordAt(paymentClosed, neha, AuditAction.FEEDBACK_RECEIVED,
                IncidentStatus.CLOSED, IncidentStatus.CLOSED, "Customer feedback received: 5/5.", now.minus(2, ChronoUnit.DAYS));
        storageClosed.recordFeedback(4, "Issue was fixed, but the initial update took a while.");
        auditService.recordAt(storageClosed, neha, AuditAction.FEEDBACK_RECEIVED,
                IncidentStatus.CLOSED, IncidentStatus.CLOSED, "Customer feedback received: 4/5.", now.minus(6, ChronoUnit.DAYS));

        // Duplicate relationship
        paymentDuplicate.markDuplicate(paymentOpen, 0.72);
        auditService.recordAt(paymentDuplicate, null, AuditAction.DUPLICATE_FLAGGED,
                IncidentStatus.CLASSIFIED, IncidentStatus.CLASSIFIED,
                "Potential duplicate of " + paymentOpen.getReference() + " (similarity 72%).",
                now.minus(8, ChronoUnit.MINUTES));
        paymentDuplicate.markResponseWarningSent();

        seedComments(paymentOpen, paymentDuplicate, databaseResolved, authAtRisk, neha, vikram, priya, rohan);
        seedNotifications(
                admin, priya, rohan, neha,
                paymentOpen, paymentDuplicate, authAtRisk, paymentClosed,
                now
        );

        log.info("Demo data ready. Accounts: admin@itsm.local / Admin@123, priya@itsm.local / Tech@123, employee@itsm.local / Employee@123");
        log.info("Seeded {} users, {} technicians, {} incidents.",
                userRepository.count(), technicians.size(), incidentRepository.count());
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> categories = new LinkedHashMap<>();
        categories.put("HARDWARE", categoryRepository.save(new Category("HARDWARE", "Hardware", "Laptops, monitors, peripherals and end-user devices.")));
        categories.put("SOFTWARE", categoryRepository.save(new Category("SOFTWARE", "Software", "Applications, installs, licensing and updates.")));
        categories.put("NETWORK", categoryRepository.save(new Category("NETWORK", "Network", "Connectivity, VPN, DNS, routing and latency.")));
        categories.put("DATABASE", categoryRepository.save(new Category("DATABASE", "Database", "Database availability, replication and queries.")));
        categories.put("CLOUD", categoryRepository.save(new Category("CLOUD", "Cloud", "Cloud infrastructure, containers and storage.")));
        categories.put("SECURITY", categoryRepository.save(new Category("SECURITY", "Security", "Phishing, malware, access and suspicious activity.")));
        categories.put("AUTHENTICATION", categoryRepository.save(new Category("AUTHENTICATION", "Authentication", "Login, SSO, MFA and identity issues.")));
        categories.put("APPLICATION", categoryRepository.save(new Category("APPLICATION", "Application", "Business applications and service outages.")));
        categories.put("STORAGE", categoryRepository.save(new Category("STORAGE", "Storage", "Disks, quotas, file shares and backups.")));
        categories.put("OTHER", categoryRepository.save(new Category("OTHER", "Other", "Uncategorised incidents requiring manual triage.")));
        return categories;
    }

    private Map<String, ItService> seedServices() {
        Map<String, ItService> services = new LinkedHashMap<>();
        services.put("PAYMENT_GATEWAY", serviceRepository.save(new ItService(
                "PAYMENT_GATEWAY", "Payment Gateway", "Customer card payments and checkout processing.", BusinessCriticality.CRITICAL)));
        services.put("IDENTITY_SSO", serviceRepository.save(new ItService(
                "IDENTITY_SSO", "Identity & SSO", "Single sign-on, MFA and identity services.", BusinessCriticality.CRITICAL)));
        services.put("DATABASE_CLUSTER", serviceRepository.save(new ItService(
                "DATABASE_CLUSTER", "Database Cluster", "Primary and replica database infrastructure.", BusinessCriticality.CRITICAL)));
        services.put("VPN", serviceRepository.save(new ItService(
                "VPN", "Remote VPN", "Remote connectivity for staff and contractors.", BusinessCriticality.HIGH)));
        services.put("EMAIL_COLLABORATION", serviceRepository.save(new ItService(
                "EMAIL_COLLABORATION", "Email & Collaboration", "Mail, calendar and collaboration tools.", BusinessCriticality.HIGH)));
        services.put("CLOUD_STORAGE", serviceRepository.save(new ItService(
                "CLOUD_STORAGE", "Cloud Storage", "Object storage and shared file services.", BusinessCriticality.HIGH)));
        services.put("HR_PORTAL", serviceRepository.save(new ItService(
                "HR_PORTAL", "HR Portal", "Employee self-service and HR workflows.", BusinessCriticality.MEDIUM)));
        services.put("END_USER_COMPUTING", serviceRepository.save(new ItService(
                "END_USER_COMPUTING", "End-User Computing", "Laptops, desktops and peripherals.", BusinessCriticality.LOW)));
        return services;
    }

    private void seedSlaPolicies(Map<String, ItService> services) {
        globalPolicies = new HashMap<>();
        globalPolicies.put(Priority.P1, slaPolicyRepository.save(new SlaPolicy(
                "P1 critical — global", Priority.P1, null, 5, 30, 80)));
        globalPolicies.put(Priority.P2, slaPolicyRepository.save(new SlaPolicy(
                "P2 high — global", Priority.P2, null, 15, 120, 80)));
        globalPolicies.put(Priority.P3, slaPolicyRepository.save(new SlaPolicy(
                "P3 medium — global", Priority.P3, null, 60, 480, 75)));
        globalPolicies.put(Priority.P4, slaPolicyRepository.save(new SlaPolicy(
                "P4 low — global", Priority.P4, null, 240, 1440, 75)));
        paymentP1Policy = slaPolicyRepository.save(new SlaPolicy(
                "P1 critical — payment gateway", Priority.P1, services.get("PAYMENT_GATEWAY"), 2, 15, 70));
    }

    private Technician tech(
            String name,
            String email,
            String password,
            int experience,
            int capacity,
            Map<String, Category> categories,
            String... skillCodes
    ) {
        Set<Category> skills = Arrays.stream(skillCodes)
                .map(categories::get)
                .collect(java.util.stream.Collectors.toSet());
        Technician technician = (Technician) userFactory.create(
                Role.TECHNICIAN, name, email, passwordEncoder.encode(password),
                null, null, null, experience, capacity, skills
        );
        return userRepository.save(technician);
    }

    private Employee employee(String name, String email, String password, String department, String phone) {
        return (Employee) userRepository.save(userFactory.create(
                Role.EMPLOYEE, name, email, passwordEncoder.encode(password),
                phone, department, null, null, null, null
        ));
    }

    private SeedBuilder seedIncident(
            String title,
            String description,
            Employee reporter,
            Category category,
            ItService service,
            Impact impact,
            Urgency urgency,
            int affectedUsers
    ) {
        return new SeedBuilder(title, description, reporter, category, service, impact, urgency, affectedUsers);
    }

    private SlaPolicy policyFor(ItService service, Priority priority) {
        if ("PAYMENT_GATEWAY".equals(service.getCode()) && priority == Priority.P1) {
            return paymentP1Policy;
        }
        return globalPolicies.get(priority);
    }

    private void seedComments(
            Incident paymentOpen,
            Incident paymentDuplicate,
            Incident databaseResolved,
            Incident authAtRisk,
            Employee neha,
            Employee vikram,
            Technician priya,
            Technician rohan
    ) {
        Instant now = clock.instant();
        addComment(paymentOpen, neha, "Checkout is failing for our customers. We have stopped the campaign until this is fixed.", false, now.minus(118, ChronoUnit.MINUTES));
        addComment(paymentOpen, priya, "Rolled back the 14:00 deployment. The gateway pods are restarting; validating health checks now.", true, now.minus(100, ChronoUnit.MINUTES));
        addComment(paymentOpen, priya, "Service is responding again. We are monitoring transaction success before resolving.", false, now.minus(45, ChronoUnit.MINUTES));
        addComment(paymentDuplicate, vikram, "Some card payments are timing out. It may be the same problem the finance team reported.", false, now.minus(8, ChronoUnit.MINUTES));
        addComment(databaseResolved, priya, "Replica recreated and WAL backlog replayed. Lag is back under one second; please confirm the reports look correct.", false, now.minus(305, ChronoUnit.MINUTES));
        addComment(authAtRisk, rohan, "Investigating token cache saturation. The identity provider is responding but slower than normal.", true, now.minus(70, ChronoUnit.MINUTES));
    }

    private void addComment(Incident incident, User author, String message, boolean internal, Instant createdAt) {
        commentRepository.save(new IncidentComment(incident, author, message, internal, createdAt));
        auditService.recordAt(
                incident,
                author,
                AuditAction.COMMENT_ADDED,
                incident.getStatus(),
                incident.getStatus(),
                (internal ? "Internal note" : "Comment") + " added: " + message,
                createdAt
        );
    }

    private void seedNotifications(
            Administrator admin,
            Technician priya,
            Technician rohan,
            Employee neha,
            Incident paymentOpen,
            Incident paymentDuplicate,
            Incident authAtRisk,
            Incident paymentClosed,
            Instant now
    ) {
        notify(admin, paymentOpen, NotificationChannel.IN_APP, NotificationType.SLA_BREACHED,
                "P1 response and resolution SLA breached",
                paymentOpen.getReference() + " has exceeded both its response and resolution targets.",
                now.minus(55, ChronoUnit.MINUTES), false);
        notify(admin, paymentDuplicate, NotificationChannel.IN_APP, NotificationType.DUPLICATE_DETECTED,
                "Possible duplicate detected",
                paymentDuplicate.getReference() + " looks similar to " + paymentOpen.getReference() + ".",
                now.minus(8, ChronoUnit.MINUTES), false);
        notify(admin, authAtRisk, NotificationChannel.EMAIL, NotificationType.SLA_WARNING,
                "SLA warning: authentication latency",
                authAtRisk.getReference() + " is approaching its resolution target.",
                now.minus(20, ChronoUnit.MINUTES), false);
        notify(priya, paymentOpen, NotificationChannel.IN_APP, NotificationType.INCIDENT_ASSIGNED,
                "P1 incident assigned to you",
                paymentOpen.getReference() + " — " + paymentOpen.getTitle(),
                now.minus(117, ChronoUnit.MINUTES), false);
        notify(rohan, authAtRisk, NotificationChannel.IN_APP, NotificationType.SLA_WARNING,
                "SLA warning on " + authAtRisk.getReference(),
                "The resolution target for the authentication latency incident is approaching.",
                now.minus(15, ChronoUnit.MINUTES), false);
        notify(neha, paymentClosed, NotificationChannel.IN_APP, NotificationType.STATUS_CHANGED,
                "Your incident was resolved",
                paymentClosed.getReference() + " was resolved and verified. Thank you for your feedback.",
                now.minus(2, ChronoUnit.DAYS), true);
    }

    private void notify(
            User recipient,
            Incident incident,
            NotificationChannel channel,
            NotificationType type,
            String title,
            String message,
            Instant createdAt,
            boolean read
    ) {
        Notification notification = new Notification(recipient, incident, channel, type, title, message, createdAt);
        if (read) {
            notification.markRead();
        }
        notificationRepository.save(notification);
    }

    private final class SeedBuilder {

        private final String title;
        private final String description;
        private final Employee reporter;
        private final Category category;
        private final ItService service;
        private final Impact impact;
        private final Urgency urgency;
        private final int affectedUsers;

        private Technician technician;
        private IncidentStatus targetStatus = IncidentStatus.CLASSIFIED;
        private long createdMinutesAgo = 30;
        private Long ackMinutesAfter;
        private Long resolveMinutesAfter;
        private Long closeMinutesAfter;
        private String resolutionNotes = "Applied the documented remediation, validated the service, and monitored for stability.";
        private String closeNotes = "Reporter confirmed the fix and the incident was closed.";

        private SeedBuilder(
                String title,
                String description,
                Employee reporter,
                Category category,
                ItService service,
                Impact impact,
                Urgency urgency,
                int affectedUsers
        ) {
            this.title = title;
            this.description = description;
            this.reporter = reporter;
            this.category = category;
            this.service = service;
            this.impact = impact;
            this.urgency = urgency;
            this.affectedUsers = affectedUsers;
        }

        private SeedBuilder technician(Technician technician) {
            this.technician = technician;
            return this;
        }

        private SeedBuilder status(IncidentStatus status) {
            this.targetStatus = status;
            return this;
        }

        private SeedBuilder createdMinutesAgo(long minutes) {
            this.createdMinutesAgo = minutes;
            return this;
        }

        private SeedBuilder ackMinutesAfter(long minutes) {
            this.ackMinutesAfter = minutes;
            return this;
        }

        private SeedBuilder resolveMinutesAfter(long minutes) {
            this.resolveMinutesAfter = minutes;
            return this;
        }

        private SeedBuilder closeMinutesAfter(long minutes) {
            this.closeMinutesAfter = minutes;
            return this;
        }

        private SeedBuilder resolutionNotes(String notes) {
            this.resolutionNotes = notes;
            return this;
        }

        private SeedBuilder closeNotes(String notes) {
            this.closeNotes = notes;
            return this;
        }

        private Incident build() {
            Instant createdAt = clock.instant().minus(createdMinutesAgo, ChronoUnit.MINUTES);
            PriorityResult priorityResult = priorityStrategy.calculate(new PriorityContext(
                    impact, urgency, service.getBusinessCriticality(), affectedUsers
            ));
            SlaPolicy policy = policyFor(service, priorityResult.priority());
            Instant responseDue = createdAt.plus(policy.getResponseMinutes(), ChronoUnit.MINUTES);
            Instant resolutionDue = createdAt.plus(policy.getResolutionMinutes(), ChronoUnit.MINUTES);
            Instant responseWarning = createdAt.plus(
                    policy.getResponseMinutes() * policy.getWarningPercent() / 100, ChronoUnit.MINUTES);
            Instant resolutionWarning = createdAt.plus(
                    policy.getResolutionMinutes() * policy.getWarningPercent() / 100, ChronoUnit.MINUTES);

            String reference = "INC-%d-%06d".formatted(
                    LocalDate.now(clock).getYear(),
                    incidentRepository.nextReferenceSequence()
            );
            Incident incident = Incident.create(
                    reference,
                    title,
                    description,
                    reporter,
                    category,
                    service,
                    impact,
                    urgency,
                    affectedUsers,
                    createdAt
            );
            incident.applyClassification(
                    priorityResult.priority(),
                    priorityResult.score(),
                    priorityResult.rationale(),
                    "APPLICATION".equals(category.getCode()),
                    0.91
            );
            incident.applySla(new com.example.itsm.sla.SlaDeadlines(
                    responseDue, resolutionDue, responseWarning, resolutionWarning
            ));
            incidentRepository.save(incident);

            auditService.recordAt(incident, reporter, AuditAction.INCIDENT_CREATED, null, IncidentStatus.NEW,
                    "Incident created by " + reporter.getName() + " for " + service.getName() + ".",
                    createdAt);
            incident.changeStatus(IncidentStatus.CLASSIFIED);
            auditService.recordAt(incident, null, AuditAction.CLASSIFIED, IncidentStatus.NEW, IncidentStatus.CLASSIFIED,
                    "Classified as " + category.getName() + " and prioritised " + priorityResult.priority().name() + ".",
                    createdAt.plus(1, ChronoUnit.MINUTES));
            auditService.recordAt(incident, null, AuditAction.PRIORITY_CALCULATED,
                    IncidentStatus.CLASSIFIED, IncidentStatus.CLASSIFIED, priorityResult.rationale(),
                    createdAt.plus(1, ChronoUnit.MINUTES));

            Instant ackAt = ackMinutesAfter == null ? null : createdAt.plus(ackMinutesAfter, ChronoUnit.MINUTES);
            Instant resolvedAt = resolveMinutesAfter == null ? null : createdAt.plus(resolveMinutesAfter, ChronoUnit.MINUTES);
            Instant closedAt = closeMinutesAfter == null ? null : createdAt.plus(closeMinutesAfter, ChronoUnit.MINUTES);

            if (technician != null) {
                incident.assignTechnician(technician);
                incident.changeStatus(IncidentStatus.ASSIGNED);
                auditService.recordAt(incident, null, AuditAction.ASSIGNED,
                        IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                        "Assigned to " + technician.getName() + " by the skill-based strategy.",
                        ackAt != null ? ackAt.minus(1, ChronoUnit.MINUTES) : createdAt.plus(5, ChronoUnit.MINUTES));
            }

            if (targetStatus == IncidentStatus.CLASSIFIED) {
                // leave unassigned for the admin queue
            } else if (targetStatus == IncidentStatus.ASSIGNED) {
                // assigned above
            } else {
                Instant ackTime = ackAt != null ? ackAt : createdAt.plus(5, ChronoUnit.MINUTES);
                boolean responseBreached = ackTime.isAfter(responseDue);
                incident.acknowledge(ackTime, responseBreached);
                incident.changeStatus(IncidentStatus.ACKNOWLEDGED);
                auditService.recordAt(incident, technician, AuditAction.ACKNOWLEDGED,
                        IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED,
                        "Acknowledged by " + (technician == null ? "the support team" : technician.getName()) + ".",
                        ackTime);
                if (responseBreached) {
                    auditService.recordAt(incident, null, AuditAction.SLA_BREACHED,
                            IncidentStatus.ACKNOWLEDGED, IncidentStatus.ACKNOWLEDGED,
                            "Response SLA breach recorded on acknowledgement.", ackTime);
                }

                if (targetStatus != IncidentStatus.ACKNOWLEDGED
                        && targetStatus != IncidentStatus.RESOLVED
                        && targetStatus != IncidentStatus.VERIFIED
                        && targetStatus != IncidentStatus.CLOSED) {
                    incident.changeStatus(IncidentStatus.IN_PROGRESS);
                    auditService.recordAt(incident, technician, AuditAction.WORK_STARTED,
                            IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS,
                            "Investigation started by " + (technician == null ? "support" : technician.getName()) + ".",
                            ackTime.plus(1, ChronoUnit.MINUTES));
                }
            }

            if (targetStatus == IncidentStatus.RESOLVED
                    || targetStatus == IncidentStatus.VERIFIED
                    || targetStatus == IncidentStatus.CLOSED) {
                Instant resolvedTime = resolvedAt != null ? resolvedAt : createdAt.plus(60, ChronoUnit.MINUTES);
                boolean resolutionBreached = resolvedTime.isAfter(resolutionDue);
                incident.resolve(resolutionNotes, resolvedTime, resolutionBreached);
                incident.changeStatus(IncidentStatus.RESOLVED);
                auditService.recordAt(incident, technician, AuditAction.RESOLVED,
                        IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
                        "Resolved: " + resolutionNotes, resolvedTime);
                if (resolutionBreached) {
                    auditService.recordAt(incident, null, AuditAction.SLA_BREACHED,
                            IncidentStatus.RESOLVED, IncidentStatus.RESOLVED,
                            "Resolution SLA breach recorded on resolution.", resolvedTime);
                }
            }

            if (targetStatus == IncidentStatus.VERIFIED || targetStatus == IncidentStatus.CLOSED) {
                Instant verifiedAt = resolvedAt != null
                        ? resolvedAt.plus(20, ChronoUnit.MINUTES)
                        : createdAt.plus(80, ChronoUnit.MINUTES);
                incident.changeStatus(IncidentStatus.VERIFIED);
                auditService.recordAt(incident, reporter, AuditAction.VERIFIED,
                        IncidentStatus.RESOLVED, IncidentStatus.VERIFIED,
                        "Resolution verified by " + reporter.getName() + ".", verifiedAt);
            }

            if (targetStatus == IncidentStatus.CLOSED) {
                Instant closedTime = closedAt != null ? closedAt : createdAt.plus(120, ChronoUnit.MINUTES);
                incident.close(closeNotes, closedTime);
                incident.changeStatus(IncidentStatus.CLOSED);
                auditService.recordAt(incident, null, AuditAction.CLOSED,
                        IncidentStatus.VERIFIED, IncidentStatus.CLOSED,
                        "Incident closed: " + closeNotes, closedTime);
            }

            Instant now = clock.instant();
            if (incident.getAcknowledgedAt() == null && now.isAfter(responseDue)) {
                incident.markResponseBreached();
                incident.markResponseWarningSent();
                auditService.recordAt(incident, null, AuditAction.SLA_BREACHED,
                        incident.getStatus(), incident.getStatus(),
                        "Response SLA deadline passed without acknowledgement.", responseDue);
            } else if (incident.getAcknowledgedAt() == null && now.isAfter(responseWarning)) {
                incident.markResponseWarningSent();
            }

            if (incident.getResolvedAt() == null && now.isAfter(resolutionDue)) {
                incident.markResolutionBreached();
                incident.markResolutionWarningSent();
                auditService.recordAt(incident, null, AuditAction.SLA_BREACHED,
                        incident.getStatus(), incident.getStatus(),
                        "Resolution SLA deadline passed without resolution.", resolutionDue);
            } else if (incident.getResolvedAt() == null && now.isAfter(resolutionWarning)) {
                incident.markResolutionWarningSent();
            }

            if (incident.getAcknowledgedAt() == null
                    && incident.getResponseDueAt() != null
                    && now.isAfter(incident.getResponseDueAt())
                    && incident.getPriority().getRank() <= Priority.P2.getRank()) {
                long minutesPast = Duration.between(incident.getResponseDueAt(), now).toMinutes();
                int level = (int) Math.min(3, 1 + minutesPast / 15);
                incident.escalate(level, now);
                auditService.recordAt(incident, null, AuditAction.ESCALATED,
                        incident.getStatus(), incident.getStatus(),
                        "Auto-escalated to level " + level + " due to unacknowledged response SLA.",
                        now);
            }

            return incident;
        }
    }
}
