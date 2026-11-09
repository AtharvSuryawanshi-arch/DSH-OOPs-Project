package com.example.itsm.user;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String phone,
        boolean active,
        String department,
        String jobTitle,
        Integer experienceYears,
        Integer maxCapacity,
        Boolean available,
        List<SkillInfo> skills,
        Instant createdAt
) {

    public record SkillInfo(Long id, String name) {
    }

    public static UserResponse from(User user) {
        String department = null;
        String jobTitle = null;
        Integer experienceYears = null;
        Integer maxCapacity = null;
        Boolean available = null;
        List<SkillInfo> skills = List.of();

        if (user instanceof Employee employee) {
            department = employee.getDepartment();
        } else if (user instanceof Technician technician) {
            experienceYears = technician.getExperienceYears();
            maxCapacity = technician.getMaxCapacity();
            available = technician.getAvailable();
            skills = technician.getSkills().stream()
                    .map(category -> new SkillInfo(category.getId(), category.getName()))
                    .toList();
        } else if (user instanceof Administrator administrator) {
            jobTitle = administrator.getJobTitle();
        }

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.isActive(),
                department,
                jobTitle,
                experienceYears,
                maxCapacity,
                available,
                skills,
                user.getCreatedAt()
        );
    }
}
