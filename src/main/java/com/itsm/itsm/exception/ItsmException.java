package com.itsm.itsm.exception;

// Base class for all our own exceptions. It extends RuntimeException
// (an "unchecked" exception), so methods don't need "throws" on every line,
// and one handler (GlobalExceptionHandler) can catch them all.
public class ItsmException extends RuntimeException {

    public ItsmException(String message) {
        super(message);
    }

    // "cause" is the original low-level error (e.g. from the database or the mail server).
    // We keep it inside our exception so the full details still appear in the log.
    public ItsmException(String message, Throwable cause) {
        super(message, cause);
    }
}
