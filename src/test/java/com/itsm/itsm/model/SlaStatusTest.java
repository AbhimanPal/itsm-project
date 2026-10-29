package com.itsm.itsm.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlaStatusTest {

    private Incident incident;

    @BeforeEach
    void setUp() {
        Employee asha = new Employee("Asha", "asha@test.com", "hash", "Finance");
        Category app = new Category("Application", "app");
        ITService payment = new ITService("Payment Application", 3, "Payments");
        incident = new Incident("Payments down", "Error 500", asha, app, payment, 3, 3, 10);
        // P1 default: respond in 5 min, resolve in 30 min
        incident.applySla(new SlaPolicy(null, Priority.P1, 5, 30));
    }

    @Test
    void deadlinesAreCountedFromCreation() {
        assertEquals(incident.getCreatedAt().plusMinutes(5), incident.getResponseDueAt());
        assertEquals(incident.getCreatedAt().plusMinutes(30), incident.getResolutionDueAt());
    }

    @Test
    void earlyOnIsOnTrack() {
        assertEquals(SlaStatus.ON_TRACK, incident.getSlaStatus(incident.getCreatedAt().plusMinutes(1)));
    }

    @Test
    void notAcknowledgedAfterResponseDeadlineIsBreached() {
        assertEquals(SlaStatus.BREACHED, incident.getSlaStatus(incident.getCreatedAt().plusMinutes(6)));
    }

    @Test
    void after80PercentOfTimeIsAtRisk() {
        acknowledgeNow();
        assertEquals(SlaStatus.AT_RISK, incident.getSlaStatus(incident.getCreatedAt().plusMinutes(24)));
    }

    @Test
    void pastResolutionDeadlineIsBreached() {
        acknowledgeNow();
        assertEquals(SlaStatus.BREACHED, incident.getSlaStatus(incident.getCreatedAt().plusMinutes(31)));
    }

    @Test
    void resolvedInTimeIsMet() {
        acknowledgeNow();
        incident.changeStatus(IncidentStatus.IN_PROGRESS);
        incident.resolve("Restarted the payment server");
        assertEquals(SlaStatus.MET, incident.getSlaStatus());
    }

    private void acknowledgeNow() {
        incident.changeStatus(IncidentStatus.CLASSIFIED);
        incident.assignTo(new Technician("Meera", "meera@test.com", "hash", 5, false));
        incident.changeStatus(IncidentStatus.ACKNOWLEDGED);
    }
}
