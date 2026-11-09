package com.example.itsm.dashboard;

import com.example.itsm.incident.dto.IncidentResponse;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        String scope,
        Totals totals,
        List<CountItem> byStatus,
        List<CountItem> byPriority,
        List<CountItem> byCategory,
        List<CountItem> byService,
        List<TechnicianLoad> technicianWorkload,
        List<TrendPoint> trend,
        List<IncidentResponse> recentIncidents,
        Instant generatedAt
) {

    public record Totals(
            long total,
            long open,
            long critical,
            long resolved,
            long slaBreaches,
            long unassigned,
            double averageResponseMinutes,
            double averageResolutionMinutes
    ) {
    }

    public record CountItem(String key, String label, long count) {
    }

    public record TechnicianLoad(
            Long id,
            String name,
            long activeIncidents,
            Integer maxCapacity,
            boolean available
    ) {
    }

    public record TrendPoint(String date, long created, long resolved) {
    }
}
