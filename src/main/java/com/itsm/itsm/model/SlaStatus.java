package com.itsm.itsm.model;

public enum SlaStatus {
    ON_TRACK("On track"),
    AT_RISK("At risk"),
    BREACHED("Breached"),
    MET("Met"),
    NO_SLA("No SLA");

    private final String label;

    SlaStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
