package com.itsm.itsm.report;

import com.itsm.itsm.model.Technician;

// A record is a short way to write a small read-only class: Java creates the
// constructor, the getters (technician(), active(), ...) and equals/toString for us.
public record TechnicianWorkload(Technician technician, long active, long resolved, long breached) {

    // How full the technician is, 0-100, for the bar on the dashboard
    public int percentFull() {
        int max = technician.getMaxActiveIncidents();
        return max == 0 ? 0 : (int) Math.min(100, active * 100 / max);
    }
}
