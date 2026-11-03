package com.itsm.itsm.exception;

import com.itsm.itsm.model.Technician;

public class WorkloadExceededException extends ItsmException {

    public WorkloadExceededException(Technician technician) {
        super(technician.getName() + " already has the maximum of "
                + technician.getMaxActiveIncidents() + " active incidents.");
    }
}
