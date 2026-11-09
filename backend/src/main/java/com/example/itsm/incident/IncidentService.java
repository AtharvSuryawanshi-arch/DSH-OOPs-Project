package com.example.itsm.incident;

import com.example.itsm.audit.AuditAction;
import com.example.itsm.audit.AuditResponse;
import com.example.itsm.audit.AuditService;
import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.CategoryRepository;
import com.example.itsm.catalog.CatalogService;
import com.example.itsm.catalog.ItService;
import com.example.itsm.common.BusinessRuleException;
import com.example.itsm.common.PageResponse;
import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.assignment.AssignmentService;
import com.example.itsm.incident.assignment.TechnicianCandidateService;
import com.example.itsm.incident.assignment.TechnicianRecommendationResponse;
import com.example.itsm.incident.dto.CancelIncidentRequest;
import com.example.itsm.incident.dto.CloseIncidentRequest;
import com.example.itsm.incident.dto.CommentResponse;
import com.example.itsm.incident.dto.CreateIncidentRequest;
import com.example.itsm.incident.dto.DuplicateResponse;
import com.example.itsm.incident.dto.FeedbackRequest;
import com.example.itsm.incident.dto.IncidentResponse;
import com.example.itsm.incident.dto.PriorityOverrideRequest;
import com.example.itsm.incident.dto.ResolveIncidentRequest;
import com.example.itsm.incident.dto.TechnicianWorkloadResponse;
import com.example.itsm.incident.dto.UpdateIncidentRequest;
import com.example.itsm.incident.lifecycle.IncidentWorkflowService;
import com.example.itsm.incident.priority.PriorityContext;
import com.example.itsm.incident.priority.PriorityResult;
import com.example.itsm.incident.priority.PriorityStrategy;
import com.example.itsm.incident.search.IncidentQuery;
import com.example.itsm.incident.search.IncidentSpecifications;
import com.example.itsm.intelligence.CategorizationService;
import com.example.itsm.intelligence.ClassificationSuggestion;
import com.example.itsm.intelligence.DuplicateDetectionService;
import com.example.itsm.intelligence.DuplicateMatch;
import com.example.itsm.intelligence.ResolutionSuggestion;
import com.example.itsm.intelligence.ResolutionSuggestionService;
import com.example.itsm.notification.NotificationEvents;
import com.example.itsm.security.CurrentUserService;
import com.example.itsm.security.UserPrincipal;
import com.example.itsm.settings.SettingsService;
import com.example.itsm.sla.SlaDeadlines;
import com.example.itsm.sla.SlaPolicyService;
import com.example.itsm.user.Role;
import com.example.itsm.user.Technician;
import com.example.itsm.user.User;
import com.example.itsm.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentCommentRepository commentRepository;
    private final IncidentMapper mapper;
    private final CurrentUserService currentUserService;
    private final CatalogService catalogService;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;
    private final PriorityStrategy priorityStrategy;
    private final SlaPolicyService slaPolicyService;
    private final IncidentWorkflowService workflowService;
    private final AssignmentService assignmentService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ResolutionSuggestionService resolutionSuggestionService;
    private final TechnicianCandidateService technicianCandidateService;
    private final AuditService auditService;
    private final SettingsService settingsService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentCommentRepository commentRepository,
            IncidentMapper mapper,
            CurrentUserService currentUserService,
            CatalogService catalogService,
            CategoryRepository categoryRepository,
            CategorizationService categorizationService,
            PriorityStrategy priorityStrategy,
            SlaPolicyService slaPolicyService,
            IncidentWorkflowService workflowService,
            AssignmentService assignmentService,
            DuplicateDetectionService duplicateDetectionService,
            ResolutionSuggestionService resolutionSuggestionService,
            TechnicianCandidateService technicianCandidateService,
            AuditService auditService,
            SettingsService settingsService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.catalogService = catalogService;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
        this.priorityStrategy = priorityStrategy;
        this.slaPolicyService = slaPolicyService;
        this.workflowService = workflowService;
        this.assignmentService = assignmentService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.resolutionSuggestionService = resolutionSuggestionService;
        this.technicianCandidateService = technicianCandidateService;
        this.auditService = auditService;
        this.settingsService = settingsService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        User reporter = currentUserService.requireUser();
        ItService service = catalogService.requireActiveService(request.serviceId());
        Instant now = clock.instant();

        List<Category> activeCategories = categoryRepository.findByActiveTrueOrderByNameAsc();
        if (activeCategories.isEmpty()) {
            throw new BusinessRuleException("NO_CATEGORIES", "No active incident categories are configured.");
        }

        boolean automatic = request.categoryId() == null;
        Category category;
        ClassificationSuggestion suggestion = null;
        double confidence = 1.0;

        if (automatic) {
            suggestion = categorizationService.suggest(request.title(), request.description(), activeCategories);
            Long suggestedId = suggestion.categoryId();
            category = activeCategories.stream()
                    .filter(candidate -> candidate.getId().equals(suggestedId))
                    .findFirst()
                    .orElse(activeCategories.getFirst());
            confidence = suggestion.confidence();
        } else {
            category = catalogService.requireActiveCategory(request.categoryId());
        }

        PriorityResult priorityResult = priorityStrategy.calculate(new PriorityContext(
                request.impact(),
                request.urgency(),
                service.getBusinessCriticality(),
                request.affectedUsers()
        ));
        SlaDeadlines deadlines = slaPolicyService.deadlinesFor(priorityResult.priority(), service, now);

        Incident incident = Incident.create(
                generateReference(),
                request.title().trim(),
                request.description().trim(),
                reporter,
                category,
                service,
                request.impact(),
                request.urgency(),
                request.affectedUsers(),
                now
        );
        incident.applyClassification(
                priorityResult.priority(),
                priorityResult.score(),
                priorityResult.rationale(),
                automatic,
                automatic ? confidence : null
        );
        incident.applySla(deadlines);
        incidentRepository.save(incident);

        auditService.record(
                incident,
                reporter,
                AuditAction.INCIDENT_CREATED,
                null,
                IncidentStatus.NEW,
                "Incident created by " + reporter.getName() + " for " + service.getName() + "."
        );

        workflowService.changeStatus(incident, IncidentStatus.CLASSIFIED);
        auditService.record(
                incident,
                null,
                AuditAction.CLASSIFIED,
                IncidentStatus.NEW,
                IncidentStatus.CLASSIFIED,
                "Classified as " + category.getName()
                        + (automatic ? " (auto-classified, " + Math.round(confidence * 100) + "% confidence)" : "")
                        + "."
        );
        auditService.record(
                incident,
                null,
                AuditAction.PRIORITY_CALCULATED,
                IncidentStatus.CLASSIFIED,
                IncidentStatus.CLASSIFIED,
                priorityResult.rationale()
        );

        List<DuplicateMatch> duplicates = duplicateDetectionService.findMatches(
                incident.getTitle(),
                incident.getDescription(),
                service.getId(),
                incident.getId()
        );
        duplicates.stream()
                .findFirst()
                .filter(match -> match.score() >= 0.5)
                .ifPresent(match -> {
                    incident.markDuplicate(match.incident(), match.score());
                    auditService.record(
                            incident,
                            null,
                            AuditAction.DUPLICATE_FLAGGED,
                            IncidentStatus.CLASSIFIED,
                            IncidentStatus.CLASSIFIED,
                            "Potential duplicate of " + match.incident().getReference()
                                    + " (similarity " + Math.round(match.score() * 100) + "%)."
                    );
                    eventPublisher.publishEvent(new NotificationEvents.DuplicateDetected(
                            incident,
                            match.incident()
                    ));
                });

        eventPublisher.publishEvent(new NotificationEvents.IncidentCreated(incident, reporter));

        if (settingsService.autoAssign()) {
            assignmentService.autoAssign(incident);
        }

        return detailResponse(incident, currentUserService.requirePrincipal());
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> search(IncidentQuery query) {
        UserPrincipal principal = currentUserService.requirePrincipal();
        Long reporterScope = principal.role() == Role.EMPLOYEE ? principal.id() : null;
        Long technicianScope = principal.role() == Role.TECHNICIAN ? principal.id() : null;

        Specification<Incident> specification = IncidentSpecifications.build(
                query,
                reporterScope,
                technicianScope,
                clock.instant()
        );
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Incident> page = incidentRepository.findAll(specification, pageable);
        return PageResponse.from(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(Long id) {
        Incident incident = load(id);
        requireView(incident, currentUserService.requirePrincipal());
        return detailResponse(incident, currentUserService.requirePrincipal());
    }

    @Transactional
    public IncidentResponse update(Long id, UpdateIncidentRequest request) {
        Incident incident = load(id);
        UserPrincipal principal = currentUserService.requirePrincipal();
        requireReporterOrAdmin(incident, principal);

        if (incident.getStatus() != IncidentStatus.NEW && incident.getStatus() != IncidentStatus.CLASSIFIED) {
            throw new BusinessRuleException(
                    "INCIDENT_NOT_EDITABLE",
                    "Incident details can only be edited before assignment."
            );
        }

        String title = request.title() == null ? incident.getTitle() : request.title().trim();
        String description = request.description() == null ? incident.getDescription() : request.description().trim();
        if (title.isBlank() || description.isBlank()) {
            throw new BusinessRuleException("INVALID_INCIDENT_DETAILS", "Title and description cannot be blank.");
        }
        Impact impact = request.impact() == null ? incident.getImpact() : request.impact();
        Urgency urgency = request.urgency() == null ? incident.getUrgency() : request.urgency();
        int affectedUsers = request.affectedUsers() == null ? incident.getAffectedUsers() : request.affectedUsers();

        incident.updateDetails(title, description, impact, urgency, affectedUsers);

        PriorityResult result = priorityStrategy.calculate(new PriorityContext(
                impact,
                urgency,
                incident.getService().getBusinessCriticality(),
                affectedUsers
        ));
        SlaDeadlines deadlines = slaPolicyService.deadlinesFor(
                result.priority(),
                incident.getService(),
                incident.getCreatedAt()
        );
        incident.applyClassification(
                result.priority(),
                result.score(),
                result.rationale(),
                incident.isAutoClassified(),
                incident.getClassificationConfidence()
        );
        incident.applySla(deadlines);

        auditService.record(
                incident,
                currentUserService.requireUser(),
                AuditAction.UPDATED,
                incident.getStatus(),
                incident.getStatus(),
                "Details updated; re-prioritised to " + result.priority().name() + " (score " + result.score() + ")."
        );

        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse assign(Long id, Long technicianId) {
        Incident incident = load(id);
        UserPrincipal principal = currentUserService.requirePrincipal();
        requireAdmin(principal);
        assignmentService.assignManually(incident, technicianId, currentUserService.requireUser());
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse acknowledge(Long id) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireAssignedTechnician(incident, actor);

        workflowService.validate(incident, IncidentStatus.ACKNOWLEDGED);
        Instant now = clock.instant();
        boolean breached = incident.isResponseBreached()
                || (incident.getResponseDueAt() != null && now.isAfter(incident.getResponseDueAt()));
        boolean newlyBreached = breached && !incident.isResponseBreached();

        IncidentStatus from = incident.getStatus();
        incident.acknowledge(now, breached);
        workflowService.changeStatus(incident, IncidentStatus.ACKNOWLEDGED);
        auditService.record(
                incident,
                actor,
                AuditAction.ACKNOWLEDGED,
                from,
                IncidentStatus.ACKNOWLEDGED,
                "Acknowledged by " + actor.getName() + (breached ? " after the response target." : " within the response target.")
        );
        if (newlyBreached) {
            auditService.record(
                    incident,
                    null,
                    AuditAction.SLA_BREACHED,
                    IncidentStatus.ACKNOWLEDGED,
                    IncidentStatus.ACKNOWLEDGED,
                    "Response SLA breach recorded on acknowledgement."
            );
            eventPublisher.publishEvent(new NotificationEvents.SlaBreached(incident, "RESPONSE"));
        }
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.ACKNOWLEDGED,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse start(Long id) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireAssignedTechnician(incident, actor);

        IncidentStatus from = incident.getStatus();
        workflowService.changeStatus(incident, IncidentStatus.IN_PROGRESS);
        auditService.record(
                incident,
                actor,
                AuditAction.WORK_STARTED,
                from,
                IncidentStatus.IN_PROGRESS,
                "Investigation started by " + actor.getName() + "."
        );
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.IN_PROGRESS,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse resolve(Long id, ResolveIncidentRequest request) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireAssignedTechnician(incident, actor);

        workflowService.validate(incident, IncidentStatus.RESOLVED);
        Instant now = clock.instant();
        boolean breached = incident.isResolutionBreached()
                || (incident.getResolutionDueAt() != null && now.isAfter(incident.getResolutionDueAt()));
        boolean newlyBreached = breached && !incident.isResolutionBreached();

        IncidentStatus from = incident.getStatus();
        incident.resolve(request.resolutionNotes().trim(), now, breached);
        workflowService.changeStatus(incident, IncidentStatus.RESOLVED);
        auditService.record(
                incident,
                actor,
                AuditAction.RESOLVED,
                from,
                IncidentStatus.RESOLVED,
                "Resolved by " + actor.getName() + ": " + excerpt(request.resolutionNotes())
        );
        if (newlyBreached) {
            auditService.record(
                    incident,
                    null,
                    AuditAction.SLA_BREACHED,
                    IncidentStatus.RESOLVED,
                    IncidentStatus.RESOLVED,
                    "Resolution SLA breach recorded on resolution."
            );
            eventPublisher.publishEvent(new NotificationEvents.SlaBreached(incident, "RESOLUTION"));
        }
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.RESOLVED,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse verify(Long id) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireReporter(incident, actor);

        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new BusinessRuleException("INCIDENT_NOT_RESOLVED", "Only a resolved incident can be verified.");
        }

        IncidentStatus from = incident.getStatus();
        workflowService.changeStatus(incident, IncidentStatus.VERIFIED);
        auditService.record(
                incident,
                actor,
                AuditAction.VERIFIED,
                from,
                IncidentStatus.VERIFIED,
                "Resolution verified by " + actor.getName() + "."
        );
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.VERIFIED,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse close(Long id, CloseIncidentRequest request) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireAdmin(currentUserService.requirePrincipal());

        if (incident.getStatus() != IncidentStatus.VERIFIED) {
            throw new BusinessRuleException(
                    "INCIDENT_NOT_VERIFIED",
                    "An incident must be verified by the reporter before it can be closed."
            );
        }

        IncidentStatus from = incident.getStatus();
        incident.close(request == null ? null : request.closureNotes(), clock.instant());
        workflowService.changeStatus(incident, IncidentStatus.CLOSED);
        auditService.record(
                incident,
                actor,
                AuditAction.CLOSED,
                from,
                IncidentStatus.CLOSED,
                "Closed by " + actor.getName()
                        + (request != null && request.closureNotes() != null ? ": " + excerpt(request.closureNotes()) : ".")
        );
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.CLOSED,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse reopen(Long id) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        if (!isReporter(incident, actor.getId()) && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the reporter or an administrator can reopen an incident.");
        }
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new BusinessRuleException("INCIDENT_NOT_REOPENABLE", "Only a resolved incident can be reopened.");
        }

        boolean breached = incident.getResolutionDueAt() != null
                && clock.instant().isAfter(incident.getResolutionDueAt());
        IncidentStatus from = incident.getStatus();
        incident.reopen(breached);
        workflowService.changeStatus(incident, IncidentStatus.IN_PROGRESS);
        auditService.record(
                incident,
                actor,
                AuditAction.REOPENED,
                from,
                IncidentStatus.IN_PROGRESS,
                "Reopened by " + actor.getName() + "."
        );
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.IN_PROGRESS,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse cancel(Long id, CancelIncidentRequest request) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireAdmin(currentUserService.requirePrincipal());

        IncidentStatus from = incident.getStatus();
        workflowService.changeStatus(incident, IncidentStatus.CANCELLED);
        incident.close(request.reason().trim(), clock.instant());
        auditService.record(
                incident,
                actor,
                AuditAction.CANCELLED,
                from,
                IncidentStatus.CANCELLED,
                "Cancelled by " + actor.getName() + ": " + excerpt(request.reason())
        );
        eventPublisher.publishEvent(new NotificationEvents.StatusChanged(
                incident,
                from,
                IncidentStatus.CANCELLED,
                actor
        ));
        return mapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponse feedback(Long id, FeedbackRequest request) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireReporter(incident, actor);

        if (!incident.getStatus().isResolved()) {
            throw new BusinessRuleException("FEEDBACK_NOT_AVAILABLE", "Feedback is available after resolution.");
        }
        if (incident.getFeedbackRating() != null) {
            throw new BusinessRuleException("FEEDBACK_ALREADY_PROVIDED", "Feedback has already been submitted.");
        }
        incident.recordFeedback(request.rating(), request.comment());
        auditService.record(
                incident,
                actor,
                AuditAction.FEEDBACK_RECEIVED,
                incident.getStatus(),
                incident.getStatus(),
                "Customer feedback received: " + request.rating() + "/5."
        );
        return mapper.toResponse(incident);
    }

    @Transactional
    public CommentResponse addComment(Long id, String message, boolean internal) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireView(incident, currentUserService.requirePrincipal());

        if (internal && actor.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees cannot create internal notes.");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessRuleException("INVALID_COMMENT", "Comment message cannot be blank.");
        }

        IncidentComment comment = commentRepository.save(new IncidentComment(
                incident,
                actor,
                message.trim(),
                internal,
                clock.instant()
        ));
        auditService.record(
                incident,
                actor,
                AuditAction.COMMENT_ADDED,
                incident.getStatus(),
                incident.getStatus(),
                (internal ? "Internal note" : "Comment") + " added: " + excerpt(message)
        );
        eventPublisher.publishEvent(new NotificationEvents.CommentAdded(incident, actor, internal));
        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> audit(Long id) {
        Incident incident = load(id);
        requireView(incident, currentUserService.requirePrincipal());
        return auditService.forIncident(id);
    }

    @Transactional(readOnly = true)
    public List<DuplicateResponse> duplicates(Long id) {
        Incident incident = load(id);
        requireView(incident, currentUserService.requirePrincipal());
        return duplicateDetectionService.findMatchesFor(id).stream()
                .map(match -> new DuplicateResponse(
                        match.incident().getId(),
                        match.incident().getReference(),
                        match.incident().getTitle(),
                        match.incident().getStatus().name(),
                        match.incident().getPriority().name(),
                        match.score(),
                        match.incident().getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResolutionSuggestion> suggestions(Long id) {
        Incident incident = load(id);
        requireView(incident, currentUserService.requirePrincipal());
        List<Incident> resolved = incidentRepository.findResolvedForSuggestions(
                IncidentStatusGroups.RESOLVED_STATUSES,
                clock.instant().minus(180, ChronoUnit.DAYS),
                incident.getId()
        );
        return resolutionSuggestionService.suggest(
                incident.getTitle(),
                incident.getDescription(),
                resolved
        );
    }

    @Transactional(readOnly = true)
    public List<TechnicianRecommendationResponse> recommendedTechnicians(Long id) {
        Incident incident = load(id);
        requireView(incident, currentUserService.requirePrincipal());
        return technicianCandidateService.recommendationsFor(incident);
    }

    @Transactional(readOnly = true)
    public TechnicianWorkloadResponse myWorkload() {
        User user = currentUserService.requireUser();
        if (!(user instanceof Technician technician)) {
            throw new AccessDeniedException("Only technicians have a workload view.");
        }
        List<Incident> active = incidentRepository
                .findByAssignedTechnicianIdAndStatusInOrderByResolutionDueAtAsc(
                        technician.getId(),
                        IncidentStatusGroups.ACTIVE_WORK
                );
        long open = incidentRepository.countByAssignedTechnicianIdAndStatusIn(
                technician.getId(),
                IncidentStatusGroups.UNRESOLVED
        );
        long resolved = incidentRepository.countByAssignedTechnicianIdAndStatusIn(
                technician.getId(),
                IncidentStatusGroups.RESOLVED_STATUSES
        );

        return new TechnicianWorkloadResponse(
                technician.getId(),
                technician.getName(),
                active.size(),
                technician.getMaxCapacity(),
                technician.isAvailable(),
                (int) open,
                (int) resolved,
                active.stream().map(mapper::toResponse).toList(),
                clock.instant()
        );
    }

    @Transactional
    public IncidentResponse overridePriority(Long id, PriorityOverrideRequest request) {
        Incident incident = load(id);
        User actor = currentUserService.requireUser();
        requireAdmin(currentUserService.requirePrincipal());

        if (incident.getStatus().isResolved() || incident.getStatus().isTerminal()) {
            throw new BusinessRuleException("PRIORITY_LOCKED", "Priority cannot be changed after resolution.");
        }

        SlaDeadlines deadlines = slaPolicyService.deadlinesFor(
                request.priority(),
                incident.getService(),
                incident.getCreatedAt()
        );
        String rationale = "Manual override to " + request.priority().name() + " by " + actor.getName()
                + (request.reason() == null || request.reason().isBlank() ? "." : ": " + request.reason().trim());
        incident.overridePriority(request.priority(), incident.getPriorityScore(), rationale, deadlines);

        if (incident.getAcknowledgedAt() != null
                && incident.getResponseDueAt() != null
                && incident.getAcknowledgedAt().isAfter(incident.getResponseDueAt())) {
            incident.markResponseBreached();
        }

        auditService.record(
                incident,
                actor,
                AuditAction.PRIORITY_OVERRIDDEN,
                incident.getStatus(),
                incident.getStatus(),
                rationale
        );
        return mapper.toResponse(incident);
    }

    private IncidentResponse detailResponse(Incident incident, UserPrincipal principal) {
        List<CommentResponse> comments = commentRepository
                .findByIncidentIdOrderByCreatedAtAsc(incident.getId())
                .stream()
                .filter(comment -> shouldShowInternal(principal) || !comment.isInternal())
                .map(CommentResponse::from)
                .toList();
        return mapper.toResponse(incident, comments, auditService.forIncident(incident.getId()));
    }

    private Incident load(Long id) {
        return incidentRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Incident", id));
    }

    private void requireView(Incident incident, UserPrincipal principal) {
        if (principal.role() == Role.ADMIN) {
            return;
        }
        if (principal.role() == Role.EMPLOYEE && isReporter(incident, principal.id())) {
            return;
        }
        if (principal.role() == Role.TECHNICIAN
                && incident.getAssignedTechnician() != null
                && incident.getAssignedTechnician().getId().equals(principal.id())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this incident.");
    }

    private void requireReporterOrAdmin(Incident incident, UserPrincipal principal) {
        if (principal.role() == Role.ADMIN || isReporter(incident, principal.id())) {
            return;
        }
        throw new AccessDeniedException("Only the reporter or an administrator can edit this incident.");
    }

    private void requireReporter(Incident incident, User actor) {
        if (!isReporter(incident, actor.getId())) {
            throw new AccessDeniedException("Only the reporter can perform this action.");
        }
    }

    private void requireAssignedTechnician(Incident incident, User actor) {
        if (actor.getRole() != Role.TECHNICIAN
                || incident.getAssignedTechnician() == null
                || !incident.getAssignedTechnician().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the assigned technician can perform this action.");
        }
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal.role() != Role.ADMIN) {
            throw new AccessDeniedException("Administrator access is required.");
        }
    }

    private boolean isReporter(Incident incident, Long userId) {
        return incident.getReporter().getId().equals(userId);
    }

    private boolean shouldShowInternal(UserPrincipal principal) {
        return principal.role() == Role.TECHNICIAN || principal.role() == Role.ADMIN;
    }

    private String generateReference() {
        long sequence = incidentRepository.nextReferenceSequence();
        return String.format(Locale.ROOT, "INC-%d-%06d", LocalDate.now(clock).getYear(), sequence);
    }

    private String excerpt(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "…";
    }
}
