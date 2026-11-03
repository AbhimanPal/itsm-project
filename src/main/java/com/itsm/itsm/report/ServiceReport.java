package com.itsm.itsm.report;

import com.itsm.itsm.model.ITService;

// One row of the "incidents per service" table
public record ServiceReport(ITService service, long total, long open, long breached) {
}
