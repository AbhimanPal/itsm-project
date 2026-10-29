package com.itsm.itsm.exception;

public class ServiceNotFoundException extends ItsmException {

    public ServiceNotFoundException(Long serviceId) {
        super("No IT service exists with id " + serviceId + ".");
    }
}
