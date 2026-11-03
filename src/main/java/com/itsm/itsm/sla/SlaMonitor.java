package com.itsm.itsm.sla;

import com.itsm.itsm.event.EventType;
import com.itsm.itsm.event.IncidentEvent;
import com.itsm.itsm.event.IncidentEventPublisher;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.repository.IncidentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// The background SLA checker. Spring calls checkEveryMinute() by itself once a minute
// (see @EnableScheduling on ItsmApplication). For every open incident it:
//   1. warns the technician once when 80% of the time is used up,
//   2. records a violation once when a deadline is missed,
//   3. escalates it if nobody acknowledged it in time.
// Every step publishes an event, so the audit trail and notifications happen automatically.
@Component
public class SlaMonitor {

    private final IncidentRepository incidentRepository;
    private final EscalationService escalationService;
    private final IncidentEventPublisher eventPublisher;

    public SlaMonitor(IncidentRepository incidentRepository, EscalationService escalationService,
                      IncidentEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.escalationService = escalationService;
        this.eventPublisher = eventPublisher;
    }

    // initialDelay: wait one minute after start-up before the first check
    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    @Transactional
    public void checkEveryMinute() {
        check(LocalDateTime.now());
    }

    // "now" is a parameter so tests can pretend time has passed. Returns how many changes were made.
    @Transactional
    public int check(LocalDateTime now) {
        int changes = 0;
        for (Incident incident : incidentRepository.findByStatusIn(IncidentStatus.OPEN)) {
            if (incident.needsSlaWarning(now)) {
                incident.markSlaWarningSent(now);
                publish(incident, EventType.SLA_WARNING, "80% of the SLA time is used up. Resolve by "
                        + incident.getResolutionDueAt().toLocalTime().withNano(0) + ".");
                changes++;
            }
            if (incident.needsBreachRecord(now)) {
                incident.recordBreach(now);
                publish(incident, EventType.SLA_BREACHED, "SLA violated: " + incident.describeBreach() + ".");
                changes++;
            }
            if (escalationService.escalateIfNeeded(incident, now)) {
                changes++;
            }
        }
        return changes;
    }

    private void publish(Incident incident, EventType type, String description) {
        eventPublisher.publish(new IncidentEvent(incident, null, type,
                incident.getStatus(), incident.getStatus(), description));
    }
}
