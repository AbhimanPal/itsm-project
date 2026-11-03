package com.itsm.itsm.duplicate;

import com.itsm.itsm.exception.DuplicateIncidentException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.repository.*;
import com.itsm.itsm.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DuplicateDetectionIntegrationTest {

    @Autowired IncidentService incidentService;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ITServiceRepository serviceRepository;

    Employee asha;
    Employee vikram;
    Long network;
    Long wifi;
    Long email;

    @BeforeEach
    void loadSeedData() {
        asha = employeeRepository.findByEmail("asha@itsm.local").orElseThrow();
        vikram = employeeRepository.findByEmail("vikram@itsm.local").orElseThrow();
        network = categoryRepository.findByNameIgnoreCase("Network").orElseThrow().getId();
        wifi = serviceRepository.findByName("Campus Wi-Fi").orElseThrow().getId();
        email = serviceRepository.findByName("Email").orElseThrow().getId();
    }

    private Incident report(Employee who, Long service, String title, String description) {
        return incidentService.reportIncident(who, title, description, network, service, 2, 2, 40);
    }

    @Test
    void similarReportFromSomeoneElseIsLinkedAndGoesToSameTechnician() {
        Incident first = report(asha, wifi, "No Wi-Fi in library", "Library Wi-Fi is down");
        Incident second = report(vikram, wifi, "Library wifi down", "Wi-Fi in the library stopped");

        assertEquals(first.getId(), second.getDuplicateOf().getId());
        assertEquals(first.getAssignedTechnician().getId(), second.getAssignedTechnician().getId());
        assertEquals(List.of(second), incidentService.findDuplicatesOf(first));
    }

    @Test
    void samePersonReportingAgainIsBlocked() {
        report(asha, wifi, "No Wi-Fi in library", "Library Wi-Fi is down");
        assertThrows(DuplicateIncidentException.class,
                () -> report(asha, wifi, "Library Wi-Fi down", "Still no Wi-Fi in the library"));
    }

    @Test
    void sameWordsOnAnotherServiceAreNotADuplicate() {
        report(asha, wifi, "No Wi-Fi in library", "Library Wi-Fi is down");
        Incident other = report(vikram, email, "No Wi-Fi in library", "Library Wi-Fi is down");
        assertNull(other.getDuplicateOf());
    }

    @Test
    void differentProblemOnSameServiceIsNotADuplicate() {
        report(asha, wifi, "No Wi-Fi in library", "Library Wi-Fi is down");
        Incident other = report(vikram, wifi, "Hostel router blinking red", "Router in hostel block C restarts");
        assertNull(other.getDuplicateOf());
    }
}
