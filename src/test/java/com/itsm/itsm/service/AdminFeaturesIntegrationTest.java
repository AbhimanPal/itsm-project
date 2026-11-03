package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.TechnicianUnavailableException;
import com.itsm.itsm.exception.WorkloadExceededException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.report.ReportService;
import com.itsm.itsm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Slice 4: admin set-up (SLA, services, technicians), manual assignment, search and reports
@SpringBootTest
@Transactional
class AdminFeaturesIntegrationTest {

    @Autowired AdminCatalogService catalogService;
    @Autowired AdminUserService userService;
    @Autowired IncidentService incidentService;
    @Autowired ReportService reportService;
    @Autowired SlaService slaService;
    @Autowired UserRepository userRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired TechnicianRepository technicianRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ITServiceRepository serviceRepository;

    Admin admin;
    Employee asha;
    Technician ravi;
    Technician meera;
    ITService wifi;

    @BeforeEach
    void loadSeedData() {
        admin = (Admin) userRepository.findByEmail("admin@itsm.local").orElseThrow();
        asha = employeeRepository.findByEmail("asha@itsm.local").orElseThrow();
        ravi = technicianRepository.findByEmail("ravi@itsm.local").orElseThrow();
        meera = technicianRepository.findByEmail("meera@itsm.local").orElseThrow();
        wifi = serviceRepository.findByName("Campus Wi-Fi").orElseThrow();
    }

    private Incident reportWifiProblem() {
        Long network = categoryRepository.findByNameIgnoreCase("Network").orElseThrow().getId();
        return incidentService.reportIncident(asha, "No Wi-Fi", "Library Wi-Fi is down", network, wifi.getId(), 2, 2, 40);
    }

    // A different problem on the same service (so duplicate detection doesn't block it)
    private Incident reportVpnProblem() {
        Long network = categoryRepository.findByNameIgnoreCase("Network").orElseThrow().getId();
        return incidentService.reportIncident(asha, "VPN fails", "Hostel VPN rejects my certificate", network,
                wifi.getId(), 2, 2, 40);
    }

    private List<Long> skillIdsOf(Technician technician) {
        return technician.getSkills().stream().map(Category::getId).toList();
    }

    // Ravi is the only Network technician; making him unavailable leaves Wi-Fi incidents unassigned
    private void makeRaviUnavailable() {
        userService.updateTechnician(ravi.getId(), 5, false, true, skillIdsOf(ravi));
    }

    @Test
    void savingSlaRuleTwiceUpdatesInsteadOfDuplicating() {
        int before = slaService.findAll().size();
        catalogService.saveSlaPolicy(wifi.getId(), Priority.P3, 30, 240);
        catalogService.saveSlaPolicy(wifi.getId(), Priority.P3, 20, 200);

        assertEquals(before + 1, slaService.findAll().size());
        assertEquals(200, slaService.findPolicy(wifi, Priority.P3).getResolutionMinutes());
    }

    @Test
    void responseLongerThanResolutionIsRejected() {
        assertThrows(InvalidInputException.class,
                () -> catalogService.saveSlaPolicy(null, Priority.P1, 60, 30));
    }

    @Test
    void defaultSlaRuleCannotBeDeleted() {
        Long defaultP1 = slaService.findAll().stream()
                .filter(p -> p.isDefault() && p.getPriority() == Priority.P1)
                .findFirst().orElseThrow().getId();
        assertThrows(InvalidInputException.class, () -> catalogService.deleteSlaPolicy(defaultP1));
    }

    @Test
    void serviceNamesMustBeUnique() {
        assertThrows(InvalidInputException.class,
                () -> catalogService.saveService(null, "email", 2, "Copy of Email"));
        assertThrows(InvalidInputException.class,
                () -> catalogService.saveService(null, "New service", 5, "Bad criticality"));
    }

    @Test
    void newTechnicianCanLogInWithHashedPassword() {
        Long storage = categoryRepository.findByNameIgnoreCase("Storage").orElseThrow().getId();
        Technician neha = userService.createTechnician("Neha", "neha@itsm.local", "secret123", 3, false,
                List.of(storage));

        assertTrue(neha.getSkills().stream().anyMatch(c -> c.getName().equals("Storage")));
        assertNotEquals("secret123", neha.getPasswordHash());
        assertThrows(InvalidInputException.class, () -> userService.createTechnician(
                "Neha again", "neha@itsm.local", "secret123", 3, false, List.of(storage)));
    }

    @Test
    void adminAssignsIncidentThatNobodyCouldTake() {
        makeRaviUnavailable();
        Incident incident = reportWifiProblem();
        assertEquals(IncidentStatus.CLASSIFIED, incident.getStatus());

        // Ravi is unavailable, so the admin can't pick him
        assertThrows(TechnicianUnavailableException.class,
                () -> incidentService.assignManually(incident.getId(), ravi.getId(), admin));

        incidentService.assignManually(incident.getId(), meera.getId(), admin);
        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
        assertEquals(meera.getId(), incident.getAssignedTechnician().getId());
    }

    @Test
    void adminCannotGoOverTechnicianWorkloadLimit() {
        makeRaviUnavailable();
        userService.updateTechnician(meera.getId(), 1, true, false, skillIdsOf(meera));
        Incident first = reportWifiProblem();
        Incident second = reportVpnProblem();

        incidentService.assignManually(first.getId(), meera.getId(), admin);
        assertThrows(WorkloadExceededException.class,
                () -> incidentService.assignManually(second.getId(), meera.getId(), admin));
    }

    @Test
    void searchFindsUnassignedIncidentsAndById() {
        makeRaviUnavailable();
        Incident stuck = reportWifiProblem();

        IncidentSearch unassigned = new IncidentSearch();
        unassigned.setTechnicianId(IncidentSearch.UNASSIGNED);
        assertTrue(incidentService.search(unassigned).contains(stuck));

        IncidentSearch byNumber = new IncidentSearch();
        byNumber.setNumber(stuck.getNumber()); // e.g. "INC0001"
        assertEquals(List.of(stuck), incidentService.search(byNumber));
    }

    @Test
    void reportsCountIncidentsPerStatusAndService() {
        reportWifiProblem();
        reportVpnProblem();
        List<Incident> all = reportService.allIncidents();

        assertEquals(2L, reportService.countByStatus(all).get(IncidentStatus.ASSIGNED));
        long wifiTotal = reportService.serviceReports(all).stream()
                .filter(r -> r.service().getId().equals(wifi.getId()))
                .findFirst().orElseThrow().total();
        assertEquals(2, wifiTotal);
        long raviActive = reportService.technicianWorkloads(all).stream()
                .filter(w -> w.technician().getId().equals(ravi.getId()))
                .findFirst().orElseThrow().active();
        assertEquals(2, raviActive);
    }
}
