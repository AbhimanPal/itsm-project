package com.itsm.itsm.service;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.event.EventType;
import com.itsm.itsm.event.IncidentEvent;
import com.itsm.itsm.event.IncidentEventPublisher;
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
    private final IncidentEventPublisher eventPublisher;

    // We only know the PriorityStrategy interface, not which class implements it.
    // Spring "injects" the implementation (ScoreBasedPriorityStrategy) here.
    private final PriorityStrategy priorityStrategy;

    public IncidentService(IncidentRepository incidentRepository, CategoryRepository categoryRepository,
                           ITServiceRepository serviceRepository, SlaService slaService,
                           AssignmentService assignmentService, IncidentEventPublisher eventPublisher,
                           PriorityStrategy priorityStrategy) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.eventPublisher = eventPublisher;
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
        incidentRepository.save(incident); // save first so the incident has an ID for the audit trail

        publish(incident, reporter, EventType.CREATED, null, IncidentStatus.CLASSIFIED,
                "Reported. Category " + category.getName() + ", priority " + incident.getPriority()
                        + ", resolve by " + incident.getResolutionDueAt().toLocalTime().withNano(0) + ".");

        try {
            Technician technician = assignmentService.assign(incident);
            publish(incident, null, EventType.ASSIGNED, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                    "Assigned to " + technician.getName() + " ("
                            + assignmentService.getActiveStrategy().getKey() + " strategy).");
        } catch (TechnicianUnavailableException e) {
            // Nobody free right now: the incident stays CLASSIFIED and the admin assigns it later
        }
        return incident;
    }

    public List<Incident> findByReporter(Employee reporter) {
        return incidentRepository.findByReporterOrderByCreatedAtDesc(reporter);
    }

    public List<Incident> findByTechnician(Technician technician) {
        return incidentRepository.findByAssignedTechnicianOrderByCreatedAtDesc(technician);
    }

    // ----- Technician actions -----

    public Incident getForTechnician(Long incidentId, Technician technician) {
        Incident incident = findIncident(incidentId);
        Technician assigned = incident.getAssignedTechnician();
        if (assigned == null || !assigned.getId().equals(technician.getId())) {
            throw new UnauthorizedOperationException("This incident is not assigned to you.");
        }
        return incident;
    }

    @Transactional
    public void acknowledge(Long incidentId, Technician technician) {
        moveTo(getForTechnician(incidentId, technician), technician, IncidentStatus.ACKNOWLEDGED, "Acknowledged.");
    }

    @Transactional
    public void startWork(Long incidentId, Technician technician) {
        moveTo(getForTechnician(incidentId, technician), technician, IncidentStatus.IN_PROGRESS, "Investigation started.");
    }

    @Transactional
    public void addNote(Long incidentId, Technician technician, String text) {
        Incident incident = getForTechnician(incidentId, technician);
        String note = requireText(text, "The note");
        incident.addNote(technician, NoteType.NOTE, note);
        publish(incident, technician, EventType.NOTE_ADDED, incident.getStatus(), incident.getStatus(), note);
    }

    @Transactional
    public void askForInfo(Long incidentId, Technician technician, String question) {
        Incident incident = getForTechnician(incidentId, technician);
        String text = requireText(question, "The question");
        incident.addNote(technician, NoteType.QUESTION, text);
        moveTo(incident, technician, IncidentStatus.AWAITING_INFO, "Asked the reporter: " + text);
    }

    @Transactional
    public void resumeWork(Long incidentId, Technician technician) {
        moveTo(getForTechnician(incidentId, technician), technician, IncidentStatus.IN_PROGRESS,
                "Resumed work without waiting for a reply.");
    }

    @Transactional
    public void resolve(Long incidentId, Technician technician, String resolution) {
        Incident incident = getForTechnician(incidentId, technician);
        String text = requireText(resolution, "The resolution");
        IncidentStatus old = incident.getStatus();
        incident.resolve(text);
        publish(incident, technician, EventType.STATUS_CHANGED, old, IncidentStatus.RESOLVED, "Resolved: " + text);
    }

    // ----- Employee (reporter) actions -----

    public Incident getForReporter(Long incidentId, Employee employee) {
        Incident incident = findIncident(incidentId);
        if (!incident.getReporter().getId().equals(employee.getId())) {
            throw new UnauthorizedOperationException("You can only view incidents you reported.");
        }
        return incident;
    }

    // Reply to the technician's question: the incident goes back to IN_PROGRESS
    @Transactional
    public void reply(Long incidentId, Employee employee, String text) {
        Incident incident = getForReporter(incidentId, employee);
        String reply = requireText(text, "The reply");
        incident.addNote(employee, NoteType.REPLY, reply);
        moveTo(incident, employee, IncidentStatus.IN_PROGRESS, "Reporter replied: " + reply);
    }

    // "Yes, it's fixed": RESOLVED -> VERIFIED -> CLOSED, with feedback
    @Transactional
    public void confirmFix(Long incidentId, Employee employee, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new InvalidInputException("Rating must be between 1 and 5.");
        }
        Incident incident = getForReporter(incidentId, employee);
        moveTo(incident, employee, IncidentStatus.VERIFIED, "Reporter confirmed the fix.");
        moveTo(incident, employee, IncidentStatus.CLOSED, "Closed after confirmation.");

        String text = comment == null ? "" : comment.trim();
        incident.giveFeedback(rating, text);
        publish(incident, employee, EventType.FEEDBACK_GIVEN, IncidentStatus.CLOSED, IncidentStatus.CLOSED,
                rating + "/5" + (text.isEmpty() ? "" : " - " + text));
    }

    // "No, it's still broken": RESOLVED -> IN_PROGRESS
    @Transactional
    public void reopen(Long incidentId, Employee employee, String reason) {
        Incident incident = getForReporter(incidentId, employee);
        String text = requireText(reason, "The reason");
        incident.addNote(employee, NoteType.REPLY, "Not fixed: " + text);
        moveTo(incident, employee, IncidentStatus.IN_PROGRESS, "Reopened by reporter: " + text);
    }

    // ----- Helpers -----

    private Incident findIncident(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidInputException("Incident " + incidentId + " does not exist."));
    }

    // Change the status (rules are checked inside Incident) and tell the listeners
    private void moveTo(Incident incident, User actor, IncidentStatus next, String description) {
        IncidentStatus old = incident.getStatus();
        incident.changeStatus(next);
        publish(incident, actor, EventType.STATUS_CHANGED, old, next, description);
    }

    private void publish(Incident incident, User actor, EventType type,
                         IncidentStatus oldStatus, IncidentStatus newStatus, String description) {
        eventPublisher.publish(new IncidentEvent(incident, actor, type, oldStatus, newStatus, description));
    }

    private String requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException(what + " cannot be empty.");
        }
        return text.trim();
    }
}
