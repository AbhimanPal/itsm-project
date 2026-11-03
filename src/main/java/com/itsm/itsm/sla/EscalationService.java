package com.itsm.itsm.sla;

import com.itsm.itsm.event.EventType;
import com.itsm.itsm.event.IncidentEvent;
import com.itsm.itsm.event.IncidentEventPublisher;
import com.itsm.itsm.model.EscalationLevel;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.model.Technician;
import com.itsm.itsm.repository.TechnicianRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

// Intelligent feature: auto-escalation. If the assigned technician does not
// acknowledge an incident before its response deadline, it is handed to a team lead.
// If the team lead also doesn't acknowledge it within one more response window,
// the admins are told. (Called every minute by SlaMonitor.)
@Service
public class EscalationService {

    private final TechnicianRepository technicianRepository;
    private final IncidentEventPublisher eventPublisher;

    public EscalationService(TechnicianRepository technicianRepository, IncidentEventPublisher eventPublisher) {
        this.technicianRepository = technicianRepository;
        this.eventPublisher = eventPublisher;
    }

    // Returns true if the incident was escalated
    public boolean escalateIfNeeded(Incident incident, LocalDateTime now) {
        // Only incidents that are assigned but not yet acknowledged can be escalated
        if (incident.getStatus() != IncidentStatus.ASSIGNED) {
            return false;
        }
        LocalDateTime due = incident.getNextEscalationDueAt();
        if (due == null || !now.isAfter(due)) {
            return false;
        }

        Technician current = incident.getAssignedTechnician();
        if (incident.getEscalationLevel() == EscalationLevel.NONE) {
            Optional<Technician> lead = findTeamLead(current);
            if (lead.isPresent()) {
                incident.reassignTo(lead.get());
                incident.escalate(EscalationLevel.TEAM_LEAD, now);
                publish(incident, current.getName() + " did not acknowledge in time. Escalated to team lead "
                        + lead.get().getName() + ".");
                return true;
            }
            // No other team lead is free, so go straight to the admin
        }
        incident.escalate(EscalationLevel.ADMIN, now);
        publish(incident, "Still not acknowledged by " + current.getName() + ". Escalated to the admin.");
        return true;
    }

    // Any available team lead except the technician who already has it
    private Optional<Technician> findTeamLead(Technician current) {
        for (Technician lead : technicianRepository.findByTeamLeadTrueAndAvailableTrue()) {
            if (!lead.getId().equals(current.getId())) {
                return Optional.of(lead);
            }
        }
        return Optional.empty();
    }

    private void publish(Incident incident, String description) {
        // actor = null: the system did it, not a person
        eventPublisher.publish(new IncidentEvent(incident, null, EventType.ESCALATED,
                IncidentStatus.ASSIGNED, IncidentStatus.ASSIGNED, description));
    }
}
