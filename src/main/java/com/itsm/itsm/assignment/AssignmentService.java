package com.itsm.itsm.assignment;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.AppSetting;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.model.Technician;
import com.itsm.itsm.repository.AppSettingRepository;
import com.itsm.itsm.repository.IncidentRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssignmentService {

    public static final String SETTING_KEY = "assignment.strategy";
    public static final String DEFAULT_STRATEGY = "SKILL";

    // Every class that implements AssignmentStrategy, looked up by its key.
    // Adding a third strategy = writing one new class; nothing here changes.
    private final Map<String, AssignmentStrategy> strategies = new LinkedHashMap<>();

    private final TechnicianRepository technicianRepository;
    private final IncidentRepository incidentRepository;
    private final AppSettingRepository settingRepository;

    public AssignmentService(List<AssignmentStrategy> allStrategies, TechnicianRepository technicianRepository,
                             IncidentRepository incidentRepository, AppSettingRepository settingRepository) {
        for (AssignmentStrategy strategy : allStrategies) {
            strategies.put(strategy.getKey(), strategy);
        }
        this.technicianRepository = technicianRepository;
        this.incidentRepository = incidentRepository;
        this.settingRepository = settingRepository;
    }

    public Collection<AssignmentStrategy> getStrategies() {
        return strategies.values();
    }

    public AssignmentStrategy getActiveStrategy() {
        String key = settingRepository.findById(SETTING_KEY)
                .map(AppSetting::getSettingValue)
                .orElse(DEFAULT_STRATEGY);
        return strategies.getOrDefault(key, strategies.get(DEFAULT_STRATEGY));
    }

    public void setActiveStrategy(String key) {
        if (!strategies.containsKey(key)) {
            throw new InvalidInputException("Unknown assignment strategy: " + key);
        }
        settingRepository.save(new AppSetting(SETTING_KEY, key));
    }

    public long workloadOf(Technician technician) {
        return incidentRepository.countByAssignedTechnicianAndStatusIn(technician, IncidentStatus.WORKING);
    }

    // Polymorphism: we call chooseTechnician() without knowing which strategy it is
    public Technician assign(Incident incident) {
        List<Technician> technicians = technicianRepository.findAll();
        Map<Technician, Long> activeCounts = new HashMap<>();
        for (Technician technician : technicians) {
            activeCounts.put(technician, workloadOf(technician));
        }
        Technician chosen = getActiveStrategy().chooseTechnician(incident, technicians, activeCounts);
        incident.assignTo(chosen);
        return chosen;
    }
}
