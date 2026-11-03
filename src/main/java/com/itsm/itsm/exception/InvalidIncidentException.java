package com.itsm.itsm.exception;

// The data for a new incident doesn't make sense (e.g. an empty title).
// The web form already checks this; IncidentFactory checks again so that
// no other code path can ever create a broken incident.
public class InvalidIncidentException extends ItsmException {

    public InvalidIncidentException(String message) {
        super(message);
    }
}
