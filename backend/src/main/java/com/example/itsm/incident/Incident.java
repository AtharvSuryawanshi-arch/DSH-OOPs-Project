package com.example.itsm.incident;

import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.ItService;
import com.example.itsm.common.AuditableEntity;
import com.example.itsm.sla.SlaDeadlines;
import com.example.itsm.user.Technician;
import com.example.itsm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ItService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(name = "priority_score", nullable = false)
    private int priorityScore;

    @Column(name = "priority_rationale", length = 500)
    private String priorityRationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    @Column(name = "affected_users", nullable = false)
    private int affectedUsers;

    @Column(name = "auto_classified", nullable = false)
    private boolean autoClassified;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "response_due_at")
    private Instant responseDueAt;

    @Column(name = "resolution_due_at")
    private Instant resolutionDueAt;

    @Column(name = "response_warning_at")
    private Instant responseWarningAt;

    @Column(name = "resolution_warning_at")
    private Instant resolutionWarningAt;

    @Column(name = "response_breached", nullable = false)
    private boolean responseBreached;

    @Column(name = "resolution_breached", nullable = false)
    private boolean resolutionBreached;

    @Column(name = "response_warning_sent", nullable = false)
    private boolean responseWarningSent;

    @Column(name = "resolution_warning_sent", nullable = false)
    private boolean resolutionWarningSent;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel;

    @Column(name = "last_escalated_at")
    private Instant lastEscalatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_of_id")
    private Incident duplicateOf;

    @Column(name = "duplicate_score")
    private Double duplicateScore;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Column(name = "closure_notes", columnDefinition = "text")
    private String closureNotes;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "feedback_comment", length = 1000)
    private String feedbackComment;

    @Version
    @Column(nullable = false)
    private long version;

    protected Incident() {
    }

    public static Incident create(
            String reference,
            String title,
            String description,
            User reporter,
            Category category,
            ItService service,
            Impact impact,
            Urgency urgency,
            int affectedUsers,
            Instant createdAt
    ) {
        Incident incident = new Incident();
        incident.reference = reference;
        incident.title = title;
        incident.description = description;
        incident.reporter = reporter;
        incident.category = category;
        incident.service = service;
        incident.impact = impact;
        incident.urgency = urgency;
        incident.priority = Priority.P3;
        incident.priorityScore = 0;
        incident.status = IncidentStatus.NEW;
        incident.affectedUsers = Math.max(1, affectedUsers);
        incident.autoClassified = false;
        incident.responseBreached = false;
        incident.resolutionBreached = false;
        incident.responseWarningSent = false;
        incident.resolutionWarningSent = false;
        incident.escalationLevel = 0;
        incident.importTimestamps(createdAt, createdAt);
        return incident;
    }

    public void applyClassification(
            Priority priority,
            int score,
            String rationale,
            boolean automatic,
            Double confidence
    ) {
        this.priority = priority;
        this.priorityScore = score;
        this.priorityRationale = rationale;
        this.autoClassified = automatic;
        this.classificationConfidence = confidence;
    }

    public void applySla(SlaDeadlines deadlines) {
        this.responseDueAt = deadlines.responseDueAt();
        this.resolutionDueAt = deadlines.resolutionDueAt();
        this.responseWarningAt = deadlines.responseWarningAt();
        this.resolutionWarningAt = deadlines.resolutionWarningAt();
    }

    public void assignTechnician(Technician technician) {
        this.assignedTechnician = technician;
    }

    public void acknowledge(Instant at, boolean breached) {
        this.acknowledgedAt = at;
        this.responseBreached = breached;
    }

    public void resolve(String notes, Instant at, boolean breached) {
        this.resolutionNotes = notes;
        this.resolvedAt = at;
        this.resolutionBreached = breached;
    }

    public void verify() {
        // State transition is coordinated by IncidentWorkflowService.
    }

    public void close(String notes, Instant at) {
        this.closureNotes = notes;
        this.closedAt = at;
    }

    public void reopen(boolean breached) {
        this.resolvedAt = null;
        this.closedAt = null;
        if (breached) {
            this.resolutionBreached = true;
        }
    }

    public void recordFeedback(int rating, String comment) {
        this.feedbackRating = rating;
        this.feedbackComment = comment;
    }

    public void changeStatus(IncidentStatus newStatus) {
        this.status = newStatus;
    }

    public void updateDetails(String title, String description, Impact impact, Urgency urgency, int affectedUsers) {
        this.title = title;
        this.description = description;
        this.impact = impact;
        this.urgency = urgency;
        this.affectedUsers = Math.max(1, affectedUsers);
    }

    public void markResponseWarningSent() {
        this.responseWarningSent = true;
    }

    public void markResolutionWarningSent() {
        this.resolutionWarningSent = true;
    }

    public void markResponseBreached() {
        this.responseBreached = true;
    }

    public void markResolutionBreached() {
        this.resolutionBreached = true;
    }

    public void escalate(int level, Instant at) {
        this.escalationLevel = level;
        this.lastEscalatedAt = at;
    }

    public void markDuplicate(Incident original, double score) {
        this.duplicateOf = original;
        this.duplicateScore = score;
    }

    public void overridePriority(Priority priority, int score, String rationale, SlaDeadlines deadlines) {
        this.priority = priority;
        this.priorityScore = score;
        this.priorityRationale = rationale;
        applySla(deadlines);
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public User getReporter() {
        return reporter;
    }

    public Category getCategory() {
        return category;
    }

    public ItService getService() {
        return service;
    }

    public Impact getImpact() {
        return impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public String getPriorityRationale() {
        return priorityRationale;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public boolean isAutoClassified() {
        return autoClassified;
    }

    public Double getClassificationConfidence() {
        return classificationConfidence;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getResponseDueAt() {
        return responseDueAt;
    }

    public Instant getResolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant getResponseWarningAt() {
        return responseWarningAt;
    }

    public Instant getResolutionWarningAt() {
        return resolutionWarningAt;
    }

    public boolean isResponseBreached() {
        return responseBreached;
    }

    public boolean isResolutionBreached() {
        return resolutionBreached;
    }

    public boolean isResponseWarningSent() {
        return responseWarningSent;
    }

    public boolean isResolutionWarningSent() {
        return resolutionWarningSent;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public Instant getLastEscalatedAt() {
        return lastEscalatedAt;
    }

    public Incident getDuplicateOf() {
        return duplicateOf;
    }

    public Double getDuplicateScore() {
        return duplicateScore;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public String getClosureNotes() {
        return closureNotes;
    }

    public Integer getFeedbackRating() {
        return feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public long getVersion() {
        return version;
    }
}
