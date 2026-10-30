package com.itsm.itsm.service;

import com.itsm.itsm.audit.AuditRecord;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.exception.InvalidStateTransitionException;
import com.itsm.itsm.exception.UnauthorizedOperationException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.notification.Notification;
import com.itsm.itsm.notification.NotificationRepository;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.EmployeeRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integration test: starts the whole app (with the seed data) on an in-memory
// database and runs real workflows through IncidentService. @Transactional
// undoes everything after each test.
@SpringBootTest
@Transactional
class IncidentWorkflowIntegrationTest {

    @Autowired IncidentService incidentService;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired TechnicianRepository technicianRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ITServiceRepository serviceRepository;
    @Autowired AuditRecordRepository auditRecordRepository;
    @Autowired NotificationRepository notificationRepository;

    Employee asha;
    Employee vikram;
    Technician ravi;
    Technician meera;

    @BeforeEach
    void loadSeedUsers() {
        asha = employeeRepository.findByEmail("asha@itsm.local").orElseThrow();
        vikram = employeeRepository.findByEmail("vikram@itsm.local").orElseThrow();
        ravi = technicianRepository.findByEmail("ravi@itsm.local").orElseThrow();
        meera = technicianRepository.findByEmail("meera@itsm.local").orElseThrow();
    }

    private Incident reportWifiProblem() {
        Long network = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Network")).findFirst().orElseThrow().getId();
        Long wifi = serviceRepository.findByName("Campus Wi-Fi").orElseThrow().getId();
        return incidentService.reportIncident(asha, "No Wi-Fi", "Library Wi-Fi is down", network, wifi, 2, 2, 40);
    }

    @Test
    void reportingAssignsTechnicianAndRecordsAuditAndNotifications() {
        Incident incident = reportWifiProblem();

        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
        assertEquals(ravi.getId(), incident.getAssignedTechnician().getId()); // only Ravi has Network
        assertNotNull(incident.getResolutionDueAt());

        List<AuditRecord> history = auditRecordRepository.findByIncidentOrderByTimestampAscIdAsc(incident);
        assertEquals(List.of("CREATED", "ASSIGNED"), history.stream().map(AuditRecord::getAction).toList());

        List<Notification> forAsha = notificationRepository.findByRecipientOrderByCreatedAtDescIdDesc(asha);
        List<Notification> forRavi = notificationRepository.findByRecipientOrderByCreatedAtDescIdDesc(ravi);
        assertEquals(2, forAsha.size()); // "received" + "assigned to Ravi"
        assertEquals(1, forRavi.size()); // "assigned to you"
    }

    @Test
    void fullWorkflowEndsClosedWithFeedback() {
        Incident incident = reportWifiProblem();
        Long id = incident.getId();

        incidentService.acknowledge(id, ravi);
        incidentService.startWork(id, ravi);
        incidentService.askForInfo(id, ravi, "Which floor?");
        incidentService.reply(id, asha, "Second floor");
        incidentService.resolve(id, ravi, "Restarted the access point");
        incidentService.confirmFix(id, asha, 5, "Quick fix");

        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertEquals(5, incident.getFeedbackRating());
        assertEquals(2, incident.getNotes().size()); // the question and the reply
        assertEquals(SlaStatus.MET, incident.getSlaStatus());
    }

    @Test
    void reporterCanReopenAResolvedIncident() {
        Long id = reportWifiProblem().getId();
        incidentService.acknowledge(id, ravi);
        incidentService.startWork(id, ravi);
        incidentService.resolve(id, ravi, "Restarted the access point");

        incidentService.reopen(id, asha, "Still no signal");

        assertEquals(IncidentStatus.IN_PROGRESS, incidentService.getForReporter(id, asha).getStatus());
    }

    @Test
    void otherTechnicianCannotActOnIncident() {
        Long id = reportWifiProblem().getId();
        assertThrows(UnauthorizedOperationException.class, () -> incidentService.acknowledge(id, meera));
    }

    @Test
    void otherEmployeeCannotSeeIncident() {
        Long id = reportWifiProblem().getId();
        assertThrows(UnauthorizedOperationException.class, () -> incidentService.getForReporter(id, vikram));
    }

    @Test
    void cannotResolveBeforeStartingWork() {
        Long id = reportWifiProblem().getId();
        assertThrows(InvalidStateTransitionException.class, () -> incidentService.resolve(id, ravi, "Fixed"));
    }
}
