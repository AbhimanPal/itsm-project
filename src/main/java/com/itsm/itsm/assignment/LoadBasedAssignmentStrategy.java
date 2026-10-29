package com.itsm.itsm.assignment;

import com.itsm.itsm.exception.TechnicianUnavailableException;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.Technician;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Ignore skills. Pick whichever available technician has the fewest active incidents.
@Component
public class LoadBasedAssignmentStrategy extends BaseAssignmentStrategy {

    @Override
    public String getKey() {
        return "LOAD";
    }

    @Override
    public String getDisplayName() {
        return "Load-based (least busy technician)";
    }

    @Override
    public Technician chooseTechnician(Incident incident, List<Technician> technicians,
                                       Map<Technician, Long> activeCounts) {
        Technician best = null;
        for (Technician technician : technicians) {
            if (!canTakeMore(technician, activeCounts)) {
                continue;
            }
            if (best == null || workloadOf(technician, activeCounts) < workloadOf(best, activeCounts)) {
                best = technician;
            }
        }
        if (best == null) {
            throw new TechnicianUnavailableException("Every technician is unavailable or at full workload.");
        }
        return best;
    }
}
