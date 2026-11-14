package com.example.itsm.incident;

import com.example.itsm.security.UserPrincipal;
import com.example.itsm.user.Role;
import org.springframework.stereotype.Component;

/**
 * The single answer to "may this person see this incident?". Used by the incident API and by every
 * feature that mentions other incidents (duplicate detection), so the rule cannot drift between them.
 *
 * <ul>
 *   <li>administrators see every incident;</li>
 *   <li>the reporter always sees their own incident, whatever their role;</li>
 *   <li>a technician sees the incidents assigned to them.</li>
 * </ul>
 */
@Component
public class IncidentAccessPolicy {

    public boolean canView(Incident incident, UserPrincipal viewer) {
        if (viewer == null || incident == null) {
            return false;
        }
        if (viewer.role() == Role.ADMIN) {
            return true;
        }
        if (incident.getReporter() != null && incident.getReporter().getId().equals(viewer.id())) {
            return true;
        }
        return viewer.role() == Role.TECHNICIAN
                && incident.getAssignedTechnician() != null
                && incident.getAssignedTechnician().getId().equals(viewer.id());
    }
}
