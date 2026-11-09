package com.example.itsm.catalog;

import com.example.itsm.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "services")
public class ItService extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_criticality", nullable = false, length = 20)
    private BusinessCriticality businessCriticality;

    @Column(nullable = false)
    private boolean active = true;

    protected ItService() {
    }

    public ItService(String code, String name, String description, BusinessCriticality businessCriticality) {
        this.code = code.toUpperCase();
        this.name = name;
        this.description = description;
        this.businessCriticality = businessCriticality;
        this.active = true;
    }

    public void update(String name, String description, BusinessCriticality businessCriticality) {
        this.name = name;
        this.description = description;
        this.businessCriticality = businessCriticality;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BusinessCriticality getBusinessCriticality() {
        return businessCriticality;
    }

    public boolean isActive() {
        return active;
    }
}
