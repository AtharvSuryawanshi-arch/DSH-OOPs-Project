package com.example.itsm.incident.assignment;

public enum AssignmentStrategyType {
    SKILL_BASED("Skill-based", "Best technical match, balanced by workload and experience."),
    LOAD_BASED("Load-based", "Least busy qualified technician first."),
    ROUND_ROBIN("Round-robin", "Distributes work across available qualified technicians.");

    private final String label;
    private final String description;

    AssignmentStrategyType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
