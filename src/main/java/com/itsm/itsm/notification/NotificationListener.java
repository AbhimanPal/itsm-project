package com.itsm.itsm.notification;

import com.itsm.itsm.event.IncidentEvent;
import com.itsm.itsm.event.IncidentListener;
import com.itsm.itsm.model.Admin;
import com.itsm.itsm.model.EscalationLevel;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.model.User;
import com.itsm.itsm.repository.AdminRepository;
import org.springframework.stereotype.Component;

import java.util.List;

// Listener 2: decides WHO should hear about an event and WHAT to tell them,
// then sends it through every channel (in-app + email).
@Component
public class NotificationListener implements IncidentListener {

    private final List<NotificationChannel> channels;
    private final AdminRepository adminRepository;

    public NotificationListener(List<NotificationChannel> channels, AdminRepository adminRepository) {
        this.channels = channels;
        this.adminRepository = adminRepository;
    }

    @Override
    public void onEvent(IncidentEvent event) {
        Incident incident = event.getIncident();
        User reporter = incident.getReporter();
        User technician = incident.getAssignedTechnician();
        String number = incident.getNumber();

        switch (event.getType()) {
            case CREATED -> notify(reporter, incident, number + " was received with priority "
                    + incident.getPriority() + ".");
            case ASSIGNED -> {
                notify(technician, incident, number + " (" + incident.getPriority() + ") was assigned to you.");
                notify(reporter, incident, number + " was assigned to " + technician.getName() + ".");
            }
            case STATUS_CHANGED -> notifyStatusChange(event, reporter, technician);
            case FEEDBACK_GIVEN -> notify(technician, incident, "Feedback on " + number + ": " + event.getDescription());
            case NOTE_ADDED -> {
                // Investigation notes are only recorded in the audit trail
            }
            case SLA_WARNING -> notify(technician, incident, "SLA warning: " + number + " " + event.getDescription());
            case SLA_BREACHED -> {
                notify(technician, incident, number + ": " + event.getDescription());
                notifyAdmins(incident, number + ": " + event.getDescription());
            }
            case DUPLICATE_DETECTED -> {
                Incident original = incident.getDuplicateOf();
                notify(reporter, incident, number + " looks like " + original.getNumber()
                        + ", which is already being handled. We linked them.");
                notify(original.getAssignedTechnician(), original, number + " was reported by "
                        + reporter.getName() + " and looks like a duplicate of " + original.getNumber() + ".");
            }
            case ESCALATED -> {
                if (incident.getEscalationLevel() == EscalationLevel.TEAM_LEAD) {
                    notify(technician, incident, number + " (" + incident.getPriority()
                            + ") was escalated to you: it was not acknowledged in time.");
                } else {
                    notifyAdmins(incident, number + " needs attention: " + event.getDescription());
                }
            }
        }
    }

    private void notifyStatusChange(IncidentEvent event, User reporter, User technician) {
        Incident incident = event.getIncident();
        String number = incident.getNumber();
        IncidentStatus from = event.getOldStatus();
        IncidentStatus to = event.getNewStatus();

        if (to == IncidentStatus.AWAITING_INFO) {
            notify(reporter, incident, "The technician has a question about " + number + ". Please reply.");
        } else if (to == IncidentStatus.IN_PROGRESS && from == IncidentStatus.AWAITING_INFO) {
            notify(technician, incident, "The reporter replied on " + number + ".");
        } else if (to == IncidentStatus.IN_PROGRESS && from == IncidentStatus.RESOLVED) {
            notify(technician, incident, number + " was reopened: the reporter says it is not fixed.");
        } else if (to == IncidentStatus.RESOLVED) {
            notify(reporter, incident, number + " was resolved. Please confirm the fix.");
        } else if (to == IncidentStatus.CLOSED) {
            notify(technician, incident, number + " was confirmed and closed by the reporter.");
        } else if (to == IncidentStatus.ACKNOWLEDGED || to == IncidentStatus.IN_PROGRESS) {
            notify(reporter, incident, number + " is now " + to + ".");
        }
    }

    private void notifyAdmins(Incident incident, String message) {
        for (Admin admin : adminRepository.findAll()) {
            notify(admin, incident, message);
        }
    }

    private void notify(User recipient, Incident incident, String message) {
        if (recipient == null) {
            return;
        }
        for (NotificationChannel channel : channels) {
            channel.send(recipient, incident, message);
        }
    }
}
