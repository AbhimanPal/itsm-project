package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.ServiceNotFoundException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.priority.PriorityStrategy;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final CategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;

    // We only know the PriorityStrategy interface, not which class implements it.
    // Spring "injects" the implementation (ScoreBasedPriorityStrategy) here.
    private final PriorityStrategy priorityStrategy;

    public IncidentService(IncidentRepository incidentRepository, CategoryRepository categoryRepository,
                           ITServiceRepository serviceRepository, PriorityStrategy priorityStrategy) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.priorityStrategy = priorityStrategy;
    }

    @Transactional
    public Incident reportIncident(Employee reporter, String title, String description, Long categoryId,
                                   Long serviceId, int impact, int urgency, int usersAffected) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidInputException("Unknown category id " + categoryId + "."));
        ITService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        Incident incident = new Incident(title, description, reporter, category, service,
                impact, urgency, usersAffected);
        incident.setPriority(priorityStrategy.calculate(impact, urgency, service.getCriticality(), usersAffected));
        incident.changeStatus(IncidentStatus.CLASSIFIED);
        return incidentRepository.save(incident);
    }

    public List<Incident> findByReporter(Employee reporter) {
        return incidentRepository.findByReporterOrderByCreatedAtDesc(reporter);
    }
}
