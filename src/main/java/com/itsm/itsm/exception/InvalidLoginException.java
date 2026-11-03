package com.itsm.itsm.exception;

// The browser is logged in as someone who doesn't exist in the database (any more),
// e.g. after the data folder was deleted while the user was still logged in.
// A wrong password on the login page is handled by Spring Security itself (?error on /login).
public class InvalidLoginException extends ItsmException {

    public InvalidLoginException(String email) {
        super("Your login (" + email + ") is no longer valid, maybe because the database was reset. "
                + "Please log out and log in again.");
    }
}
