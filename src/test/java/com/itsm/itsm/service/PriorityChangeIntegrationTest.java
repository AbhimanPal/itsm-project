package com.itsm.itsm.service;

import com.itsm.itsm.audit.AuditRecord;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.notification.NotificationRepository;
import com.itsm.itsm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

// Slice 7: the admin changes an incident's priority by hand
@SpringBootTest
@Transactional
class PriorityChangeIntegrationTest {

    @Autowired IncidentService incidentService;
    @Autowired UserRepository userRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ITServiceRepository serviceRepository;
    @Autowired AuditRecordRepository auditRecordRepository;
    @Autowired NotificationRepository notificationRepository;

    Admin admin;
    Employee asha;

    @BeforeEach
    void loadSeedData() {
        admin = (Admin) userRepository.findByEmail("admin@itsm.local").orElseThrow();
        asha = employeeRepository.findByEmail("asha@itsm.local").orElseThrow();
    }

    // 2 x 2 x criticality 2 = 8 -> P3 (respond 1 h, resolve 8 h). Goes to Ravi.
    private Incident reportWifiProblem() {
        Long network = categoryRepository.findByNameIgnoreCase("Network").orElseThrow().getId();
        Long wifi = serviceRepository.findByName("Campus Wi-Fi").orElseThrow().getId();
        return incidentService.reportIncident(asha, "No Wi-Fi", "Library Wi-Fi is down", network, wifi, 2, 2, 40);
    }

    @Test
    void raisingPriorityShortensDeadlinesAndTellsEveryone() {
        Incident incident = reportWifiProblem();
        assertEquals(Priority.P3, incident.getPriority());
        long ashaBefore = notificationRepository.findByRecipientOrderByCreatedAtDescIdDesc(asha).size();

        incidentService.changePriority(incident.getId(), Priority.P1, "Whole library affected", admin);

        assertEquals(Priority.P1, incident.getPriority());
        // Default P1 rule: 5 / 30 minutes, counted from when it was reported
        assertEquals(incident.getCreatedAt().plusMinutes(5), incident.getResponseDueAt());
        assertEquals(incident.getCreatedAt().plusMinutes(30), incident.getResolutionDueAt());
        assertTrue(auditRecordRepository.findByIncidentOrderByTimestampAscIdAsc(incident).stream()
                .map(AuditRecord::getAction).anyMatch("PRIORITY_CHANGED"::equals));
        assertEquals(ashaBefore + 1, notificationRepository.findByRecipientOrderByCreatedAtDescIdDesc(asha).size());
    }

    @Test
    void samePriorityOrEmptyReasonIsRejected() {
        Long id = reportWifiProblem().getId();
        assertThrows(InvalidInputException.class, () -> incidentService.changePriority(id, Priority.P3, "Why not", admin));
        assertThrows(InvalidInputException.class, () -> incidentService.changePriority(id, Priority.P1, "  ", admin));
    }
}
