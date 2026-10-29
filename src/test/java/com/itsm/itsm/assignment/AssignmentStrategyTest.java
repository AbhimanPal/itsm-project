package com.itsm.itsm.assignment;

import com.itsm.itsm.exception.TechnicianUnavailableException;
import com.itsm.itsm.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentStrategyTest {

    private final Category network = new Category("Network", "wifi");
    private final Category hardware = new Category("Hardware", "laptop");

    private Technician ravi;   // Network
    private Technician meera;  // Network
    private Technician karan;  // Hardware
    private Map<Technician, Long> load;
    private Incident wifiIncident;

    @BeforeEach
    void setUp() {
        ravi = new Technician("Ravi", "ravi@test.com", "hash", 5, true);
        ravi.addSkill(network);
        meera = new Technician("Meera", "meera@test.com", "hash", 5, false);
        meera.addSkill(network);
        karan = new Technician("Karan", "karan@test.com", "hash", 2, false);
        karan.addSkill(hardware);

        load = new HashMap<>();
        load.put(ravi, 3L);
        load.put(meera, 1L);
        load.put(karan, 0L);

        Employee asha = new Employee("Asha", "asha@test.com", "hash", "Finance");
        ITService wifi = new ITService("Campus Wi-Fi", 2, "Wi-Fi");
        wifiIncident = new Incident("No wifi", "Wi-Fi down", asha, network, wifi, 2, 2, 10);
    }

    @Test
    void skillBasedPicksLeastBusyTechnicianWithTheSkill() {
        Technician chosen = new SkillBasedAssignmentStrategy()
                .chooseTechnician(wifiIncident, List.of(ravi, meera, karan), load);
        assertEquals(meera, chosen); // Karan is less busy but has no Network skill
    }

    @Test
    void loadBasedIgnoresSkills() {
        Technician chosen = new LoadBasedAssignmentStrategy()
                .chooseTechnician(wifiIncident, List.of(ravi, meera, karan), load);
        assertEquals(karan, chosen);
    }

    @Test
    void unavailableTechnicianIsSkipped() {
        meera.setAvailable(false);
        Technician chosen = new SkillBasedAssignmentStrategy()
                .chooseTechnician(wifiIncident, List.of(ravi, meera, karan), load);
        assertEquals(ravi, chosen);
    }

    @Test
    void technicianAtMaxWorkloadIsSkipped() {
        load.put(karan, 2L); // Karan's max is 2
        Technician chosen = new LoadBasedAssignmentStrategy()
                .chooseTechnician(wifiIncident, List.of(ravi, meera, karan), load);
        assertEquals(meera, chosen);
    }

    @Test
    void noSkilledTechnicianThrows() {
        assertThrows(TechnicianUnavailableException.class, () -> new SkillBasedAssignmentStrategy()
                .chooseTechnician(wifiIncident, List.of(karan), load));
    }
}
