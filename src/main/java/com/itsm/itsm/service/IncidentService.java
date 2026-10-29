package com.itsm.itsm.service;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.ServiceNotFoundException;
import com.itsm.itsm.exception.TechnicianUnavailableException;
import com.itsm.itsm.exception.UnauthorizedOperationException;
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
    private final SlaService slaService;
    private final AssignmentService assignmentService;

    // We only know the PriorityStrategy interface, not which class implements it.
    // Spring "injects" the implementation (ScoreBasedPriorityStrategy) here.
    private final PriorityStrategy priorityStrategy;

    public IncidentService(IncidentRepository incidentRepository, CategoryRepository categoryRepository,
                           ITServiceRepository serviceRepository, SlaService slaService,
                           AssignmentService assignmentService, PriorityStrategy priorityStrategy) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.priorityStrategy = priorityStrategy;
    }

    // Employee reports a problem: create -> priority -> classify -> SLA -> assign
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
        incident.applySla(slaService.findPolicy(service, incident.getPriority()));

        try {
            assignmentService.assign(incident);
        } catch (TechnicianUnavailableException e) {
            // Nobody free right now: the incident stays CLASSIFIED and the admin assigns it later
        }
        return incidentRepository.save(incident);
    }

    public List<Incident> findByReporter(Employee reporter) {
        return incidentRepository.findByReporterOrderByCreatedAtDesc(reporter);
    }

    public List<Incident> findByTechnician(Technician technician) {
        return incidentRepository.findByAssignedTechnicianOrderByCreatedAtDesc(technician);
    }

    // ----- Technician actions -----

    public Incident getForTechnician(Long incidentId, Technician technician) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidInputException("Incident " + incidentId + " does not exist."));
        Technician assigned = incident.getAssignedTechnician();
        if (assigned == null || !assigned.getId().equals(technician.getId())) {
            throw new UnauthorizedOperationException("This incident is not assigned to you.");
        }
        return incident;
    }

    @Transactional
    public void acknowledge(Long incidentId, Technician technician) {
        getForTechnician(incidentId, technician).changeStatus(IncidentStatus.ACKNOWLEDGED);
    }

    @Transactional
    public void startWork(Long incidentId, Technician technician) {
        getForTechnician(incidentId, technician).changeStatus(IncidentStatus.IN_PROGRESS);
    }

    @Transactional
    public void addNote(Long incidentId, Technician technician, String text) {
        getForTechnician(incidentId, technician).addNote(technician, NoteType.NOTE, requireText(text, "The note"));
    }

    @Transactional
    public void askForInfo(Long incidentId, Technician technician, String question) {
        Incident incident = getForTechnician(incidentId, technician);
        String text = requireText(question, "The question");
        incident.changeStatus(IncidentStatus.AWAITING_INFO);
        incident.addNote(technician, NoteType.QUESTION, text);
    }

    @Transactional
    public void resumeWork(Long incidentId, Technician technician) {
        getForTechnician(incidentId, technician).changeStatus(IncidentStatus.IN_PROGRESS);
    }

    @Transactional
    public void resolve(Long incidentId, Technician technician, String resolution) {
        getForTechnician(incidentId, technician).resolve(requireText(resolution, "The resolution"));
    }

    private String requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException(what + " cannot be empty.");
        }
        return text.trim();
    }
}
