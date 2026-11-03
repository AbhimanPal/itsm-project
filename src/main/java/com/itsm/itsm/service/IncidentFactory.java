package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidIncidentException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.priority.PriorityStrategy;
import org.springframework.stereotype.Component;

// Factory pattern: the one place that knows how to build a new incident.
// Making a correct incident takes several steps (check the data, calculate the
// priority, mark it CLASSIFIED, apply the SLA). Callers just ask the factory for
// an incident and always get a complete, valid one back.
@Component
public class IncidentFactory {

    private final PriorityStrategy priorityStrategy;
    private final SlaService slaService;

    public IncidentFactory(PriorityStrategy priorityStrategy, SlaService slaService) {
        this.priorityStrategy = priorityStrategy;
        this.slaService = slaService;
    }

    public Incident create(Employee reporter, String title, String description, Category category,
                           ITService service, int impact, int urgency, int usersAffected) {
        if (title == null || title.isBlank()) {
            throw new InvalidIncidentException("An incident needs a title.");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidIncidentException("An incident needs a description.");
        }

        Incident incident = new Incident(title.trim(), description.trim(), reporter, category, service,
                impact, urgency, usersAffected);
        // The strategy also checks impact, urgency and users affected (InvalidInputException)
        incident.setPriority(priorityStrategy.calculate(impact, urgency, service.getCriticality(), usersAffected));
        incident.changeStatus(IncidentStatus.CLASSIFIED);
        incident.applySla(slaService.findPolicy(service, incident.getPriority()));
        return incident;
    }
}
