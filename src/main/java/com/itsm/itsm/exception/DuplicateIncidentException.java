package com.itsm.itsm.exception;

import com.itsm.itsm.model.Incident;

// Thrown when an employee reports the same problem they already reported and is still open
public class DuplicateIncidentException extends ItsmException {

    public DuplicateIncidentException(Incident original) {
        super("You already reported this problem as " + original.getNumber()
                + " and it is still being worked on. You can add details there instead.");
    }
}
