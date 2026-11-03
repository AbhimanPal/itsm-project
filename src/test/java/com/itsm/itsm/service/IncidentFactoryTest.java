package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidIncidentException;
import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.priority.ScoreBasedPriorityStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Slice 7: the Factory pattern. A plain unit test, no Spring and no database.
class IncidentFactoryTest {

    IncidentFactory factory;
    Employee asha = new Employee("Asha", "asha@test.com", "hash", "Finance");
    Category network = new Category("Network", "wifi");
    ITService wifi = new ITService("Campus Wi-Fi", 2, "Wi-Fi");

    @BeforeEach
    void setUp() {
        // A fake SlaService that always gives 15 / 120 minutes, so we don't need a database.
        // It is a subclass that overrides findPolicy: polymorphism in a test.
        SlaService fixedSla = new SlaService(null) {
            @Override
            public SlaPolicy findPolicy(ITService service, Priority priority) {
                return new SlaPolicy(null, priority, 15, 120);
            }
        };
        factory = new IncidentFactory(new ScoreBasedPriorityStrategy(), fixedSla);
    }

    @Test
    void createsAClassifiedIncidentWithPriorityAndDeadlines() {
        Incident incident = factory.create(asha, "  No Wi-Fi  ", "Library Wi-Fi is down", network, wifi, 3, 3, 10);

        assertEquals("No Wi-Fi", incident.getTitle());               // spaces trimmed
        assertEquals(Priority.P1, incident.getPriority());           // 3 x 3 x criticality 2 = 18
        assertEquals(IncidentStatus.CLASSIFIED, incident.getStatus());
        assertEquals(incident.getCreatedAt().plusMinutes(15), incident.getResponseDueAt());
        assertEquals(incident.getCreatedAt().plusMinutes(120), incident.getResolutionDueAt());
    }

    @Test
    void blankTitleOrDescriptionIsRejected() {
        assertThrows(InvalidIncidentException.class,
                () -> factory.create(asha, "   ", "Library Wi-Fi is down", network, wifi, 2, 2, 1));
        assertThrows(InvalidIncidentException.class,
                () -> factory.create(asha, "No Wi-Fi", null, network, wifi, 2, 2, 1));
    }

    @Test
    void impactOutOfRangeIsRejected() {
        assertThrows(InvalidInputException.class,
                () -> factory.create(asha, "No Wi-Fi", "Library Wi-Fi is down", network, wifi, 5, 2, 1));
    }
}
