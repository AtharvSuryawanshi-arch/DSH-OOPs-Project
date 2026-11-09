package com.example.itsm.user;

import com.example.itsm.catalog.Category;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column
    private Boolean available;

    @ManyToMany
    @JoinTable(
            name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> skills = new LinkedHashSet<>();

    protected Technician() {
    }

    public Technician(
            String name,
            String email,
            String passwordHash,
            String phone,
            Integer experienceYears,
            Integer maxCapacity,
            Set<Category> skills
    ) {
        super(name, email, passwordHash, phone);
        this.experienceYears = experienceYears == null ? 0 : experienceYears;
        this.maxCapacity = maxCapacity == null ? 8 : maxCapacity;
        this.available = true;
        replaceSkills(skills);
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    public List<Category> getSkills() {
        return skills.stream().sorted(Comparator.comparing(Category::getName)).toList();
    }

    public void replaceSkills(Set<Category> newSkills) {
        this.skills.clear();
        if (newSkills != null) {
            this.skills.addAll(newSkills);
        }
    }

    public boolean hasSkill(Category category) {
        return category != null && skills.stream().anyMatch(skill -> skill.getId().equals(category.getId()));
    }

    public void updateProfessionalProfile(Integer experienceYears, Integer maxCapacity, Boolean available) {
        if (experienceYears != null) {
            this.experienceYears = experienceYears;
        }
        if (maxCapacity != null) {
            this.maxCapacity = maxCapacity;
        }
        if (available != null) {
            this.available = available;
        }
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }
}
