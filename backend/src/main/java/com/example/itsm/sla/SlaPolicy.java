package com.example.itsm.sla;

import com.example.itsm.common.AuditableEntity;
import com.example.itsm.incident.Priority;
import com.example.itsm.catalog.ItService;
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

@Entity
@Table(name = "sla_policies")
public class SlaPolicy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ItService service;

    @Column(name = "response_minutes", nullable = false)
    private int responseMinutes;

    @Column(name = "resolution_minutes", nullable = false)
    private int resolutionMinutes;

    @Column(name = "warning_percent", nullable = false)
    private int warningPercent;

    @Column(nullable = false)
    private boolean active = true;

    protected SlaPolicy() {
    }

    public SlaPolicy(
            String name,
            Priority priority,
            ItService service,
            int responseMinutes,
            int resolutionMinutes,
            int warningPercent
    ) {
        this.name = name;
        this.priority = priority;
        this.service = service;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.warningPercent = warningPercent;
        this.active = true;
    }

    public void update(
            String name,
            Priority priority,
            ItService service,
            int responseMinutes,
            int resolutionMinutes,
            int warningPercent
    ) {
        this.name = name;
        this.priority = priority;
        this.service = service;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.warningPercent = warningPercent;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Priority getPriority() {
        return priority;
    }

    public ItService getService() {
        return service;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }

    public int getWarningPercent() {
        return warningPercent;
    }

    public boolean isActive() {
        return active;
    }
}
