package com.example.itsm.sla;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SlaStatusResolver {

    public SlaStatus resolve(Incident incident, Instant now) {
        if (incident.getStatus() == IncidentStatus.CANCELLED) {
            return SlaStatus.MET;
        }

        if (incident.getStatus().isResolved()) {
            return incident.isResponseBreached() || incident.isResolutionBreached()
                    ? SlaStatus.BREACHED
                    : SlaStatus.MET;
        }

        if (incident.isResponseBreached() || incident.isResolutionBreached()) {
            return SlaStatus.BREACHED;
        }

        if (incident.getAcknowledgedAt() == null
                && incident.getResponseDueAt() != null
                && now.isAfter(incident.getResponseDueAt())) {
            return SlaStatus.BREACHED;
        }

        if (incident.getResolutionDueAt() != null
                && now.isAfter(incident.getResolutionDueAt())
                && incident.getResolvedAt() == null) {
            return SlaStatus.BREACHED;
        }

        boolean responseWarning = incident.getAcknowledgedAt() == null
                && incident.getResponseWarningAt() != null
                && now.isAfter(incident.getResponseWarningAt());
        boolean resolutionWarning = incident.getResolvedAt() == null
                && incident.getResolutionWarningAt() != null
                && now.isAfter(incident.getResolutionWarningAt());

        return responseWarning || resolutionWarning ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }
}
