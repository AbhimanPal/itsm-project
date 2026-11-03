package com.itsm.itsm.exception;

// Talking to another server over the network failed, e.g. Gmail's mail server
// (no internet, wrong app password, blocked port).
public class NetworkFailureException extends ItsmException {

    public NetworkFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
