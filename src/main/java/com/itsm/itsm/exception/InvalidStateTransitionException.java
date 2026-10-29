package com.itsm.itsm.exception;

import com.itsm.itsm.model.IncidentStatus;

public class InvalidStateTransitionException extends ItsmException {

    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to) {
        super("An incident cannot move from " + from + " to " + to + ".");
    }
}
