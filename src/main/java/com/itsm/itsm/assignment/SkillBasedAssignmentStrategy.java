package com.itsm.itsm.assignment;

import com.itsm.itsm.exception.TechnicianUnavailableException;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.Technician;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Pick a technician who has the skill for the incident's category.
// If several have it, pick the one with the fewest active incidents.
@Component
public class SkillBasedAssignmentStrategy extends BaseAssignmentStrategy {

    @Override
    public String getKey() {
        return "SKILL";
    }

    @Override
    public String getDisplayName() {
        return "Skill-based (matching skill, then least busy)";
    }

    @Override
    public Technician chooseTechnician(Incident incident, List<Technician> technicians,
                                       Map<Technician, Long> activeCounts) {
        Technician best = null;
        for (Technician technician : technicians) {
            if (!technician.hasSkill(incident.getCategory()) || !canTakeMore(technician, activeCounts)) {
                continue;
            }
            if (best == null || workloadOf(technician, activeCounts) < workloadOf(best, activeCounts)) {
                best = technician;
            }
        }
        if (best == null) {
            throw new TechnicianUnavailableException("No available technician has the "
                    + incident.getCategory().getName() + " skill.");
        }
        return best;
    }
}
