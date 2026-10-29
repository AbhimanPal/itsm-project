package com.itsm.itsm.assignment;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.Technician;

import java.util.List;
import java.util.Map;

// Strategy pattern: different rules for picking a technician.
// The admin can switch between them while the app is running.
public interface AssignmentStrategy {

    // Short code saved in the database, e.g. "SKILL"
    String getKey();

    // Name shown to the admin
    String getDisplayName();

    // activeCounts = how many incidents each technician is working on right now
    Technician chooseTechnician(Incident incident, List<Technician> technicians, Map<Technician, Long> activeCounts);
}
