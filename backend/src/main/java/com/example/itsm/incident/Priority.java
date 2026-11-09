package com.example.itsm.incident;

public enum Priority {
    P1(1, "Critical"),
    P2(2, "High"),
    P3(3, "Medium"),
    P4(4, "Low");

    private final int rank;
    private final String label;

    Priority(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int getRank() {
        return rank;
    }

    public String getLabel() {
        return label;
    }

    public boolean isCritical() {
        return this == P1;
    }
}
