package com.itsm.itsm.model;

// Who is responsible after an incident was not acknowledged in time:
// first the assigned technician, then a team lead, then the admin.
public enum EscalationLevel {
    NONE("Not escalated"),
    TEAM_LEAD("Escalated to team lead"),
    ADMIN("Escalated to admin");

    private final String label;

    EscalationLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
