package com.itsm.itsm.model;

import java.util.List;

// The incident lifecycle. Each status knows which statuses may come next,
// so illegal jumps like NEW -> CLOSED can be blocked in one place.
public enum IncidentStatus {
    NEW,
    CLASSIFIED,
    ASSIGNED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    AWAITING_INFO,
    RESOLVED,
    VERIFIED,
    CLOSED;

    // Statuses where a technician is actively responsible. Used to count workload.
    public static final List<IncidentStatus> WORKING = List.of(ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, AWAITING_INFO);

    public boolean canMoveTo(IncidentStatus next) {
        return switch (this) {
            case NEW -> next == CLASSIFIED;
            case CLASSIFIED -> next == ASSIGNED;
            case ASSIGNED -> next == ACKNOWLEDGED;
            case ACKNOWLEDGED -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == AWAITING_INFO || next == RESOLVED;
            case AWAITING_INFO -> next == IN_PROGRESS;
            case RESOLVED -> next == VERIFIED || next == IN_PROGRESS; // IN_PROGRESS = reopened
            case VERIFIED -> next == CLOSED;
            case CLOSED -> false;
        };
    }

    public boolean isOpen() {
        return this != RESOLVED && this != VERIFIED && this != CLOSED;
    }
}
