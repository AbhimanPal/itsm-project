package com.itsm.itsm.exception;

// Saving to or reading from the database failed. Wraps Spring's DataAccessException,
// so the user sees our friendly message instead of a technical database error.
public class DatabaseFailureException extends ItsmException {

    public DatabaseFailureException(String whatFailed, Throwable cause) {
        super(whatFailed + " because of a database problem. Please try again.", cause);
    }
}
