package com.itsm.itsm.service;

import com.itsm.itsm.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Unit test: no database, just one Incident object checked against different filters
class IncidentSearchTest {

    private Incident incident;
    private IncidentSearch search;

    @BeforeEach
    void setUp() {
        Employee asha = new Employee("Asha", "asha@test.com", "hash", "Finance");
        Category network = new Category("Network", "wifi");
        ITService wifi = new ITService("Campus Wi-Fi", 2, "Wireless");
        incident = new Incident("No Wi-Fi", "Library Wi-Fi down", asha, network, wifi, 2, 2, 40);
        incident.setPriority(Priority.P2);
        search = new IncidentSearch();
    }

    @Test
    void emptySearchMatchesEverything() {
        assertTrue(search.matches(incident));
    }

    @Test
    void priorityAndStatusMustBothMatch() {
        search.setPriority(Priority.P2);
        search.setStatus(IncidentStatus.NEW);
        assertTrue(search.matches(incident));

        search.setStatus(IncidentStatus.CLOSED);
        assertFalse(search.matches(incident));
    }

    @Test
    void dateRangeIncludesBothEnds() {
        LocalDate today = incident.getCreatedAt().toLocalDate();
        search.setFrom(today);
        search.setTo(today);
        assertTrue(search.matches(incident));

        search.setFrom(today.plusDays(1));
        assertFalse(search.matches(incident));
    }

    @Test
    void unassignedFilterFindsIncidentsWithoutTechnician() {
        search.setTechnicianId(IncidentSearch.UNASSIGNED);
        assertTrue(search.matches(incident));
    }

    @Test
    void slaFilterUsesCalculatedStatus() {
        search.setSlaStatus(SlaStatus.NO_SLA); // no policy applied yet
        assertTrue(search.matches(incident));

        incident.applySla(new SlaPolicy(null, Priority.P2, 15, 120));
        assertFalse(search.matches(incident));
    }

    @Test
    void textThatIsNotAnIdMatchesNothing() {
        search.setNumber("hello");
        assertFalse(search.matches(incident));
    }
}
