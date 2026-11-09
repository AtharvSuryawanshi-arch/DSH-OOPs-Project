package com.example.itsm.incident;

import com.example.itsm.audit.AuditResponse;
import com.example.itsm.common.PageResponse;
import com.example.itsm.incident.assignment.TechnicianRecommendationResponse;
import com.example.itsm.incident.dto.AssignIncidentRequest;
import com.example.itsm.incident.dto.CancelIncidentRequest;
import com.example.itsm.incident.dto.CloseIncidentRequest;
import com.example.itsm.incident.dto.CommentResponse;
import com.example.itsm.incident.dto.CreateIncidentRequest;
import com.example.itsm.incident.dto.DuplicateResponse;
import com.example.itsm.incident.dto.FeedbackRequest;
import com.example.itsm.incident.dto.IncidentResponse;
import com.example.itsm.incident.dto.PriorityOverrideRequest;
import com.example.itsm.incident.dto.ResolveIncidentRequest;
import com.example.itsm.incident.dto.UpdateIncidentRequest;
import com.example.itsm.incident.search.IncidentQuery;
import com.example.itsm.intelligence.ResolutionSuggestion;
import com.example.itsm.sla.SlaStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public IncidentResponse create(@Valid @RequestBody CreateIncidentRequest request) {
        return incidentService.create(request);
    }

    @GetMapping
    public PageResponse<IncidentResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) SlaStatus slaStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "false") boolean duplicatesOnly,
            @RequestParam(defaultValue = "false") boolean escalatedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return incidentService.search(new IncidentQuery(
                search,
                status,
                priority,
                categoryId,
                serviceId,
                technicianId,
                reporterId,
                slaStatus,
                from,
                to,
                duplicatesOnly,
                escalatedOnly,
                page,
                size
        ));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id) {
        return incidentService.get(id);
    }

    @PatchMapping("/{id}")
    public IncidentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateIncidentRequest request) {
        return incidentService.update(id, request);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignIncidentRequest request) {
        return incidentService.assign(id, request.technicianId());
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public IncidentResponse acknowledge(@PathVariable Long id) {
        return incidentService.acknowledge(id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public IncidentResponse start(@PathVariable Long id) {
        return incidentService.start(id);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public IncidentResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveIncidentRequest request) {
        return incidentService.resolve(id, request);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public IncidentResponse verify(@PathVariable Long id) {
        return incidentService.verify(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse close(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid CloseIncidentRequest request
    ) {
        return incidentService.close(id, request);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public IncidentResponse reopen(@PathVariable Long id) {
        return incidentService.reopen(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelIncidentRequest request) {
        return incidentService.cancel(id, request);
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public IncidentResponse feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request) {
        return incidentService.feedback(id, request);
    }

    @PostMapping("/{id}/priority-override")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse overridePriority(
            @PathVariable Long id,
            @Valid @RequestBody PriorityOverrideRequest request
    ) {
        return incidentService.overridePriority(id, request);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request) {
        return incidentService.addComment(id, request.message(), request.internal());
    }

    @GetMapping("/{id}/audit")
    public List<AuditResponse> audit(@PathVariable Long id) {
        return incidentService.audit(id);
    }

    @GetMapping("/{id}/duplicates")
    public List<DuplicateResponse> duplicates(@PathVariable Long id) {
        return incidentService.duplicates(id);
    }

    @GetMapping("/{id}/suggested-resolutions")
    public List<ResolutionSuggestion> suggestions(@PathVariable Long id) {
        return incidentService.suggestions(id);
    }

    @GetMapping("/{id}/recommended-technicians")
    public List<TechnicianRecommendationResponse> recommendations(@PathVariable Long id) {
        return incidentService.recommendedTechnicians(id);
    }
}
