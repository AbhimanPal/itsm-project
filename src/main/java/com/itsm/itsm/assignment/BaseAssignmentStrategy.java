package com.itsm.itsm.assignment;

import com.itsm.itsm.model.Technician;

import java.util.Map;

// Abstract class: holds the helper methods both strategies share,
// so we don't write them twice. It can't be used on its own.
public abstract class BaseAssignmentStrategy implements AssignmentStrategy {

    protected long workloadOf(Technician technician, Map<Technician, Long> activeCounts) {
        return activeCounts.getOrDefault(technician, 0L);
    }

    // Available and not already at their maximum number of incidents
    protected boolean canTakeMore(Technician technician, Map<Technician, Long> activeCounts) {
        return technician.isAvailable()
                && workloadOf(technician, activeCounts) < technician.getMaxActiveIncidents();
    }
}
