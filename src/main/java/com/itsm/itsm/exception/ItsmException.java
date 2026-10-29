package com.itsm.itsm.exception;

// Base class for all our own exceptions. It extends RuntimeException
// (an "unchecked" exception), so methods don't need "throws" on every line,
// and one handler (GlobalExceptionHandler) can catch them all.
public class ItsmException extends RuntimeException {

    public ItsmException(String message) {
        super(message);
    }
}
