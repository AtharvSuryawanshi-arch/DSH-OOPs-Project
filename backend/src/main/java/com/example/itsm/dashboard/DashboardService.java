package com.example.itsm.dashboard;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentMapper;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatusGroups;
import com.example.itsm.incident.Priority;
import com.example.itsm.security.CurrentUserService;
import com.example.itsm.security.UserPrincipal;
import com.example.itsm.sla.SlaStatus;
import com.example.itsm.sla.SlaStatusResolver;
import com.example.itsm.user.Role;
import com.example.itsm.user.Technician;
import com.example.itsm.user.User;
import com.example.itsm.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;
    private final SlaStatusResolver slaStatusResolver;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final Clock clock;

    public DashboardService(
            IncidentRepository incidentRepository,
            IncidentMapper incidentMapper,
            SlaStatusResolver slaStatusResolver,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            Clock clock
    ) {
        this.incidentRepository = incidentRepository;
        this.incidentMapper = incidentMapper;
        this.slaStatusResolver = slaStatusResolver;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        UserPrincipal principal = currentUserService.requirePrincipal();
        List<Incident> incidents = scopedIncidents(principal);
        Instant now = clock.instant();

        long total = incidents.size();
        long open = incidents.stream().filter(i -> IncidentStatusGroups.UNRESOLVED.contains(i.getStatus())).count();
        long critical = incidents.stream()
                .filter(i -> i.getPriority() == Priority.P1)
                .filter(i -> IncidentStatusGroups.UNRESOLVED.contains(i.getStatus()))
                .count();
        long resolved = incidents.stream().filter(i -> i.getStatus().isResolved()).count();
        long breaches = incidents.stream()
                .filter(i -> slaStatusResolver.resolve(i, now) == SlaStatus.BREACHED)
                .count();
        long unassigned = incidents.stream()
                .filter(i -> i.getAssignedTechnician() == null)
                .filter(i -> IncidentStatusGroups.UNRESOLVED.contains(i.getStatus()))
                .count();

        double averageResponse = incidents.stream()
                .filter(i -> i.getAcknowledgedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getAcknowledgedAt()).toSeconds())
                .average()
                .orElse(0) / 60.0;
        double averageResolution = incidents.stream()
                .filter(i -> i.getResolvedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toSeconds())
                .average()
                .orElse(0) / 60.0;

        DashboardResponse.Totals totals = new DashboardResponse.Totals(
                total,
                open,
                critical,
                resolved,
                breaches,
                unassigned,
                round(averageResponse),
                round(averageResolution)
        );

        List<DashboardResponse.CountItem> byStatus = countBy(
                incidents,
                incident -> incident.getStatus().name(),
                incident -> incident.getStatus().getLabel()
        );
        List<DashboardResponse.CountItem> byPriority = countByPriority(incidents);
        List<DashboardResponse.CountItem> byCategory = countBy(
                incidents,
                incident -> String.valueOf(incident.getCategory().getId()),
                incident -> incident.getCategory().getName()
        );
        List<DashboardResponse.CountItem> byService = countBy(
                incidents,
                incident -> String.valueOf(incident.getService().getId()),
                incident -> incident.getService().getName()
        );

        List<DashboardResponse.TechnicianLoad> workload = principal.role() == Role.ADMIN
                ? technicianWorkload()
                : List.of();

        return new DashboardResponse(
                principal.role().name(),
                totals,
                byStatus,
                byPriority,
                byCategory,
                byService,
                workload,
                trend(incidents),
                incidents.stream()
                        .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                        .limit(5)
                        .map(incidentMapper::toResponse)
                        .toList(),
                now
        );
    }

    private List<Incident> scopedIncidents(UserPrincipal principal) {
        return switch (principal.role()) {
            case ADMIN -> incidentRepository.findAllDetailed();
            case EMPLOYEE -> incidentRepository.findByReporterIdOrderByCreatedAtDesc(principal.id());
            case TECHNICIAN -> incidentRepository.findByAssignedTechnicianIdOrderByCreatedAtDesc(principal.id());
        };
    }

    private List<DashboardResponse.CountItem> countByPriority(List<Incident> incidents) {
        Map<Priority, Long> counts = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            counts.put(priority, 0L);
        }
        incidents.forEach(incident -> counts.merge(incident.getPriority(), 1L, Long::sum));
        return counts.entrySet().stream()
                .map(entry -> new DashboardResponse.CountItem(
                        entry.getKey().name(),
                        entry.getKey().getLabel(),
                        entry.getValue()
                ))
                .toList();
    }

    private List<DashboardResponse.CountItem> countBy(
            List<Incident> incidents,
            java.util.function.Function<Incident, String> key,
            java.util.function.Function<Incident, String> label
    ) {
        Map<String, DashboardResponse.CountItem> counts = new LinkedHashMap<>();
        for (Incident incident : incidents) {
            String keyValue = key.apply(incident);
            DashboardResponse.CountItem current = counts.get(keyValue);
            if (current == null) {
                counts.put(keyValue, new DashboardResponse.CountItem(keyValue, label.apply(incident), 1));
            } else {
                counts.put(keyValue, new DashboardResponse.CountItem(
                        current.key(),
                        current.label(),
                        current.count() + 1
                ));
            }
        }
        return counts.values().stream()
                .sorted(Comparator.comparingLong(DashboardResponse.CountItem::count).reversed()
                        .thenComparing(DashboardResponse.CountItem::label))
                .toList();
    }

    private List<DashboardResponse.TechnicianLoad> technicianWorkload() {
        List<DashboardResponse.TechnicianLoad> workload = new ArrayList<>();
        for (User user : userRepository.findAllByOrderByNameAsc()) {
            if (user instanceof Technician technician) {
                long active = incidentRepository.countByAssignedTechnicianIdAndStatusIn(
                        technician.getId(),
                        IncidentStatusGroups.ACTIVE_WORK
                );
                workload.add(new DashboardResponse.TechnicianLoad(
                        technician.getId(),
                        technician.getName(),
                        active,
                        technician.getMaxCapacity(),
                        technician.isAvailable()
                ));
            }
        }
        return workload;
    }

    private List<DashboardResponse.TrendPoint> trend(List<Incident> incidents) {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {
            counts.put(today.minusDays(daysAgo), new long[2]);
        }
        for (Incident incident : incidents) {
            LocalDate created = LocalDate.ofInstant(incident.getCreatedAt(), ZoneOffset.UTC);
            long[] createdCounts = counts.get(created);
            if (createdCounts != null) {
                createdCounts[0]++;
            }
            if (incident.getResolvedAt() != null) {
                LocalDate resolvedDate = LocalDate.ofInstant(incident.getResolvedAt(), ZoneOffset.UTC);
                long[] resolvedCounts = counts.get(resolvedDate);
                if (resolvedCounts != null) {
                    resolvedCounts[1]++;
                }
            }
        }

        return counts.entrySet().stream()
                .map(entry -> new DashboardResponse.TrendPoint(
                        entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        entry.getValue()[0],
                        entry.getValue()[1]
                ))
                .toList();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
