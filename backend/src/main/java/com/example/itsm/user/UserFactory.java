package com.example.itsm.user;

import com.example.itsm.catalog.Category;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    public User create(
            Role role,
            String name,
            String email,
            String passwordHash,
            String phone,
            String department,
            String jobTitle,
            Integer experienceYears,
            Integer maxCapacity,
            Set<Category> skills
    ) {
        return switch (role) {
            case EMPLOYEE -> new Employee(name, email, passwordHash, phone, department);
            case TECHNICIAN -> new Technician(
                    name,
                    email,
                    passwordHash,
                    phone,
                    experienceYears,
                    maxCapacity,
                    skills
            );
            case ADMIN -> new Administrator(name, email, passwordHash, phone, jobTitle);
        };
    }
}
