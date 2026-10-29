package com.itsm.itsm.model;

import com.itsm.itsm.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentLifecycleTest {

    private Incident newIncident() {
        Employee reporter = new Employee("Asha", "asha@test.com", "hash", "Finance");
        Category category = new Category("Application", "app");
        ITService service = new ITService("Payment Application", 3, "Payments");
        return new Incident("Payments down", "Cannot pay", reporter, category, service, 3, 3, 10);
    }

    @Test
    void newIncidentStartsAsNew() {
        assertEquals(IncidentStatus.NEW, newIncident().getStatus());
    }

    @Test
    void fullHappyPathReachesClosed() {
        Incident incident = newIncident();
        incident.changeStatus(IncidentStatus.CLASSIFIED);
        incident.changeStatus(IncidentStatus.ASSIGNED);
        incident.changeStatus(IncidentStatus.ACKNOWLEDGED);
        incident.changeStatus(IncidentStatus.IN_PROGRESS);
        incident.changeStatus(IncidentStatus.RESOLVED);
        incident.changeStatus(IncidentStatus.VERIFIED);
        incident.changeStatus(IncidentStatus.CLOSED);

        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertNotNull(incident.getClosedAt());
    }

    @Test
    void newToClosedIsBlocked() {
        Incident incident = newIncident();
        assertThrows(InvalidStateTransitionException.class,
                () -> incident.changeStatus(IncidentStatus.CLOSED));
        assertEquals(IncidentStatus.NEW, incident.getStatus()); // unchanged
    }

    @Test
    void awaitingInfoGoesBackToInProgress() {
        assertTrue(IncidentStatus.IN_PROGRESS.canMoveTo(IncidentStatus.AWAITING_INFO));
        assertTrue(IncidentStatus.AWAITING_INFO.canMoveTo(IncidentStatus.IN_PROGRESS));
        assertFalse(IncidentStatus.AWAITING_INFO.canMoveTo(IncidentStatus.RESOLVED));
    }

    @Test
    void resolvedCanBeReopened() {
        assertTrue(IncidentStatus.RESOLVED.canMoveTo(IncidentStatus.IN_PROGRESS));
    }

    @Test
    void closedIsFinal() {
        for (IncidentStatus next : IncidentStatus.values()) {
            assertFalse(IncidentStatus.CLOSED.canMoveTo(next));
        }
    }
}
