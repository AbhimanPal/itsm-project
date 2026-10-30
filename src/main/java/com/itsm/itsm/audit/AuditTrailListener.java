package com.itsm.itsm.audit;

import com.itsm.itsm.event.IncidentEvent;
import com.itsm.itsm.event.IncidentListener;
import org.springframework.stereotype.Component;

// Listener 1: writes every event into the audit trail
@Component
public class AuditTrailListener implements IncidentListener {

    private final AuditRecordRepository auditRecordRepository;

    public AuditTrailListener(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @Override
    public void onEvent(IncidentEvent event) {
        String actor = event.getActor() == null
                ? "System"
                : event.getActor().getName() + " (" + event.getActor().getRole() + ")";
        auditRecordRepository.save(new AuditRecord(actor, event.getType().name(), event.getIncident(),
                event.getOldStatus(), event.getNewStatus(), event.getDescription()));
    }
}
