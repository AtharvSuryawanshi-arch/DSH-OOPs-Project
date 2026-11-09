package com.example.itsm.incident.priority;

public interface PriorityStrategy {

    PriorityResult calculate(PriorityContext context);

    String name();
}
