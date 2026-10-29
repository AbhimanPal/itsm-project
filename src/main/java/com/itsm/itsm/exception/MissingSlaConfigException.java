package com.itsm.itsm.exception;

import com.itsm.itsm.model.Priority;

public class MissingSlaConfigException extends ItsmException {

    public MissingSlaConfigException(Priority priority) {
        super("No SLA policy is configured for priority " + priority + ".");
    }
}
