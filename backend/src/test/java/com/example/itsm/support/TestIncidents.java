package com.example.itsm.support;

import com.example.itsm.catalog.BusinessCriticality;
import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.ItService;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.Impact;
import com.example.itsm.incident.Urgency;
import com.example.itsm.user.Employee;

import java.time.Instant;

public final class TestIncidents {

    public static final Instant CREATED = Instant.parse("2026-11-09T10:00:00Z");

    private TestIncidents() {
    }

    public static Incident unclassified() {
        Employee reporter = new Employee("Test Reporter", "reporter@example.test", "hash", null, "Finance");
        Category category = new Category("APPLICATION", "Application", "Application incidents");
        ItService service = new ItService(
                "PAYMENT",
                "Payment Gateway",
                "Payments",
                BusinessCriticality.CRITICAL
        );
        return Incident.create(
                "INC-TEST-0001",
                "Payment application unavailable",
                "Customers cannot complete payments.",
                reporter,
                category,
                service,
                Impact.HIGH,
                Urgency.HIGH,
                100,
                CREATED
        );
    }
}
