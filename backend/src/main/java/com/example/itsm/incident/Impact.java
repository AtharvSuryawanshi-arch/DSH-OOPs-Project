package com.example.itsm.incident;

public enum Impact {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    EXTREME(4, "Extreme");

    private final int weight;
    private final String label;

    Impact(int weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    public int getWeight() {
        return weight;
    }

    public String getLabel() {
        return label;
    }
}
