package com.itsm.itsm.sla;

import com.itsm.itsm.audit.AuditRecord;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.model.*;
import com.itsm.itsm.notification.NotificationRepository;
import com.itsm.itsm.repository.*;
import com.itsm.itsm.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Slice 5: the SLA scheduler and auto-escalation. Instead of waiting for real minutes,
// each test calls check() with a "now" in the future.
@SpringBootTest
@Transactional
class SlaMonitorIntegrationTest {

    @Autowired SlaMonitor slaMonitor;
    @Autowired IncidentService incidentService;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired TechnicianRepository technicianRepository;
    @Autowired AdminRepository adminRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ITServiceRepository serviceRepository;
    @Autowired AuditRecordRepository auditRecordRepository;
    @Autowired NotificationRepository notificationRepository;

    Employee asha;
    Technician ravi;   // team lead, Network
    Technician meera;  // Application
    Admin admin;

    @BeforeEach
    void loadSeedData() {
        asha = employeeRepository.findByEmail("asha@itsm.local").orElseThrow();
        ravi = technicianRepository.findByEmail("ravi@itsm.local").orElseThrow();
        meera = technicianRepository.findByEmail("meera@itsm.local").orElseThrow();
        admin = adminRepository.findAll().get(0);
    }

    // P1 (3 x 3 x criticality 3): respond within 5 min, resolve within 30 min. Goes to Meera.
    private Incident reportPaymentOutage() {
        Long application = categoryRepository.findByNameIgnoreCase("Application").orElseThrow().getId();
        Long payment = serviceRepository.findByName("Payment Application").orElseThrow().getId();
        return incidentService.reportIncident(asha, "Payments down", "Error 500 on fee payment",
                application, payment, 3, 3, 20);
    }

    private List<String> actions(Incident incident) {
        return auditRecordRepository.findByIncidentOrderByTimestampAscIdAsc(incident).stream()
                .map(AuditRecord::getAction).toList();
    }

    private long notificationsFor(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDescIdDesc(user).size();
    }

    @Test
    void warnsOnceWhen80PercentOfTimeIsUsed() {
        Incident incident = reportPaymentOutage();
        incidentService.acknowledge(incident.getId(), meera); // responded in time
        LocalDateTime at25min = incident.getCreatedAt().plusMinutes(25);

        assertEquals(1, slaMonitor.check(at25min));
        assertEquals(0, slaMonitor.check(at25min.plusSeconds(30))); // no second warning
        assertEquals(1, actions(incident).stream().filter("SLA_WARNING"::equals).count());
    }

    @Test
    void breachIsRecordedOnceAndAdminIsTold() {
        Incident incident = reportPaymentOutage();
        incidentService.acknowledge(incident.getId(), meera);
        long adminBefore = notificationsFor(admin);

        slaMonitor.check(incident.getCreatedAt().plusMinutes(31)); // past the 30-minute deadline
        slaMonitor.check(incident.getCreatedAt().plusMinutes(32));

        assertNotNull(incident.getSlaBreachedAt());
        assertEquals(1, actions(incident).stream().filter("SLA_BREACHED"::equals).count());
        assertEquals(adminBefore + 1, notificationsFor(admin));
    }

    @Test
    void notAcknowledgedGoesToTeamLeadThenAdmin() {
        Incident incident = reportPaymentOutage();
        assertEquals(meera.getId(), incident.getAssignedTechnician().getId());
        LocalDateTime created = incident.getCreatedAt();

        // 6 min: response deadline (5 min) missed -> team lead Ravi takes over
        slaMonitor.check(created.plusMinutes(6));
        assertEquals(EscalationLevel.TEAM_LEAD, incident.getEscalationLevel());
        assertEquals(ravi.getId(), incident.getAssignedTechnician().getId());

        // 10 min: Ravi still has until 6 + 5 = 11 min
        slaMonitor.check(created.plusMinutes(10));
        assertEquals(EscalationLevel.TEAM_LEAD, incident.getEscalationLevel());

        // 12 min: Ravi didn't acknowledge either -> admin
        long adminBefore = notificationsFor(admin);
        slaMonitor.check(created.plusMinutes(12));
        assertEquals(EscalationLevel.ADMIN, incident.getEscalationLevel());
        assertTrue(notificationsFor(admin) > adminBefore);
    }

    @Test
    void acknowledgedIncidentIsNotEscalated() {
        Incident incident = reportPaymentOutage();
        incidentService.acknowledge(incident.getId(), meera);

        slaMonitor.check(incident.getCreatedAt().plusMinutes(6));
        assertEquals(EscalationLevel.NONE, incident.getEscalationLevel());
        assertEquals(meera.getId(), incident.getAssignedTechnician().getId());
    }

    @Test
    void unassignedIncidentGoesToAdmin() {
        // Every technician is away, so nobody can be assigned
        for (Technician t : technicianRepository.findAll()) {
            t.setAvailable(false);
        }
        Incident incident = reportPaymentOutage();
        assertNull(incident.getAssignedTechnician());

        slaMonitor.check(incident.getCreatedAt().plusMinutes(6)); // past the 5-minute response deadline
        assertEquals(EscalationLevel.ADMIN, incident.getEscalationLevel());
        assertEquals(1, actions(incident).stream().filter("ESCALATED"::equals).count());
    }

    @Test
    void teamLeadsOwnIncidentGoesStraightToAdmin() {
        // Wi-Fi + Network goes to Ravi, who is the only team lead
        Long network = categoryRepository.findByNameIgnoreCase("Network").orElseThrow().getId();
        Long wifi = serviceRepository.findByName("Campus Wi-Fi").orElseThrow().getId();
        Incident incident = incidentService.reportIncident(asha, "No Wi-Fi", "Library Wi-Fi down",
                network, wifi, 2, 2, 40);

        slaMonitor.check(incident.getResponseDueAt().plusMinutes(1));
        assertEquals(EscalationLevel.ADMIN, incident.getEscalationLevel());
        assertEquals(ravi.getId(), incident.getAssignedTechnician().getId());
    }
}
