package com.itsm.itsm.model;

public enum Priority {
    P1("Critical"),
    P2("High"),
    P3("Medium"),
    P4("Low");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // P4 -> P3 -> P2 -> P1. P1 stays P1.
    public Priority oneLevelHigher() {
        return switch (this) {
            case P1, P2 -> P1;
            case P3 -> P2;
            case P4 -> P3;
        };
    }
}
