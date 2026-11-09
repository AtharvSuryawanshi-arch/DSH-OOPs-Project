package com.example.itsm.incident;

public enum IncidentStatus {
    NEW("New", false),
    CLASSIFIED("Classified", false),
    ASSIGNED("Assigned", false),
    ACKNOWLEDGED("Acknowledged", false),
    IN_PROGRESS("In progress", false),
    RESOLVED("Resolved", false),
    VERIFIED("Verified", false),
    CLOSED("Closed", true),
    CANCELLED("Cancelled", true);

    private final String label;
    private final boolean terminal;

    IncidentStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isResolved() {
        return this == RESOLVED || this == VERIFIED || this == CLOSED;
    }

    public boolean isActiveWork() {
        return this != RESOLVED && this != VERIFIED && this != CLOSED && this != CANCELLED;
    }
}
