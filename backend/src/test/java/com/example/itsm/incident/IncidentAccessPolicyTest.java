package com.example.itsm.incident;

import com.example.itsm.security.UserPrincipal;
import com.example.itsm.support.TestIncidents;
import com.example.itsm.user.Role;
import com.example.itsm.user.Technician;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentAccessPolicyTest {

    private final IncidentAccessPolicy policy = new IncidentAccessPolicy();

    private static UserPrincipal viewer(long id, Role role) {
        return new UserPrincipal(id, "Viewer " + id, "viewer" + id + "@example.test", "hash", role, true);
    }

    private static Incident incidentReportedBy(long reporterId, Long technicianId) {
        Incident incident = TestIncidents.unclassified();
        ReflectionTestUtils.setField(incident.getReporter(), "id", reporterId);
        if (technicianId != null) {
            Technician technician = mock(Technician.class);
            when(technician.getId()).thenReturn(technicianId);
            incident.assignTechnician(technician);
        }
        return incident;
    }

    @Test
    void administratorsSeeEverything() {
        assertThat(policy.canView(incidentReportedBy(1L, 2L), viewer(99L, Role.ADMIN))).isTrue();
    }

    @Test
    void reportersSeeTheirOwnIncidentWhateverTheirRole() {
        Incident incident = incidentReportedBy(5L, 2L);
        assertThat(policy.canView(incident, viewer(5L, Role.EMPLOYEE))).isTrue();
        assertThat(policy.canView(incidentReportedBy(7L, 2L), viewer(7L, Role.TECHNICIAN))).isTrue();
    }

    @Test
    void techniciansSeeOnlyWhatIsAssignedToThem() {
        Incident incident = incidentReportedBy(1L, 2L);
        assertThat(policy.canView(incident, viewer(2L, Role.TECHNICIAN))).isTrue();
        assertThat(policy.canView(incident, viewer(3L, Role.TECHNICIAN))).isFalse();
    }

    @Test
    void otherEmployeesSeeNothing() {
        assertThat(policy.canView(incidentReportedBy(1L, 2L), viewer(4L, Role.EMPLOYEE))).isFalse();
        // An employee id that happens to equal the technician's id must not grant access.
        assertThat(policy.canView(incidentReportedBy(1L, 2L), viewer(2L, Role.EMPLOYEE))).isFalse();
    }
}
