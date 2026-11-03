package com.itsm.itsm.service;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.duplicate.DuplicateDetector;
import com.itsm.itsm.duplicate.DuplicateMatch;
import com.itsm.itsm.event.EventType;
import com.itsm.itsm.event.IncidentEvent;
import com.itsm.itsm.event.IncidentEventPublisher;
import com.itsm.itsm.exception.DatabaseFailureException;
import com.itsm.itsm.exception.DuplicateIncidentException;
import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.ServiceNotFoundException;
import com.itsm.itsm.exception.TechnicianUnavailableException;
import com.itsm.itsm.exception.UnauthorizedOperationException;
import com.itsm.itsm.exception.WorkloadExceededException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.IncidentRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final CategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final SlaService slaService;
    private final AssignmentService assignmentService;
    private final IncidentEventPublisher eventPublisher;
    private final TechnicianRepository technicianRepository;
    private final DuplicateDetector duplicateDetector;
    private final IncidentFactory incidentFactory;

    public IncidentService(IncidentRepository incidentRepository, CategoryRepository categoryRepository,
                           ITServiceRepository serviceRepository, SlaService slaService,
                           AssignmentService assignmentService, IncidentEventPublisher eventPublisher,
                           TechnicianRepository technicianRepository,
                           DuplicateDetector duplicateDetector, IncidentFactory incidentFactory) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.eventPublisher = eventPublisher;
        this.technicianRepository = technicianRepository;
        this.duplicateDetector = duplicateDetector;
        this.incidentFactory = incidentFactory;
    }

    // Employee reports a problem: duplicate check -> create (factory) -> save -> assign
    @Transactional
    public Incident reportIncident(Employee reporter, String title, String description, Long categoryId,
                                   Long serviceId, int impact, int urgency, int usersAffected) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidInputException("Unknown category id " + categoryId + "."));
        ITService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        // Look for duplicates before saving, so the new report isn't compared with itself.
        // The same person reporting the same open problem again is blocked.
        Optional<DuplicateMatch> match = duplicateDetector.findDuplicate(service, title, description);
        if (match.isPresent() && match.get().original().getReporter().getId().equals(reporter.getId())) {
            throw new DuplicateIncidentException(match.get().original());
        }

        // Factory pattern: IncidentFactory builds it with priority, CLASSIFIED status and SLA deadlines
        Incident incident = incidentFactory.create(reporter, title, description, category, service,
                impact, urgency, usersAffected);
        match.ifPresent(m -> incident.markAsDuplicateOf(m.original()));
        try {
            incidentRepository.save(incident); // save first so the incident has an ID for the audit trail
        } catch (DataAccessException e) {
            // DataAccessException is Spring's general "the database said no" error
            throw new DatabaseFailureException("Your incident could not be saved", e);
        }

        publish(incident, reporter, EventType.CREATED, null, IncidentStatus.CLASSIFIED,
                "Reported. Category " + category.getName() + ", priority " + incident.getPriority()
                        + ", resolve by " + incident.getResolutionDueAt().toLocalTime().withNano(0) + ".");
        match.ifPresent(m -> publish(incident, null, EventType.DUPLICATE_DETECTED,
                IncidentStatus.CLASSIFIED, IncidentStatus.CLASSIFIED,
                "Possible duplicate of " + m.original().getNumber() + " (" + m.percent()
                        + "% of words match, same service)."));

        try {
            // Correlation: a duplicate goes to whoever already works on the original, if they have room
            Technician technician = match.isPresent() ? assignToSameTechnician(incident, match.get().original()) : null;
            String how;
            if (technician != null) {
                how = "same technician as " + match.get().original().getNumber();
            } else {
                technician = assignmentService.assign(incident);
                how = assignmentService.getActiveStrategy().getKey() + " strategy";
            }
            publish(incident, null, EventType.ASSIGNED, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                    "Assigned to " + technician.getName() + " (" + how + ").");
        } catch (TechnicianUnavailableException e) {
            // Nobody free right now: the incident stays CLASSIFIED and the admin assigns it later
        }
        return incident;
    }

    // Returns the technician, or null if the original's technician can't take it
    private Technician assignToSameTechnician(Incident incident, Incident original) {
        Technician technician = original.getAssignedTechnician();
        if (technician == null || !technician.isAvailable()
                || assignmentService.workloadOf(technician) >= technician.getMaxActiveIncidents()) {
            return null;
        }
        incident.assignTo(technician);
        return technician;
    }

    // Other reports that were linked to this one as possible duplicates
    public List<Incident> findDuplicatesOf(Incident original) {
        return incidentRepository.findByDuplicateOfOrderByCreatedAtAsc(original);
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

    // ----- Admin actions -----

    // Every incident that passes the filters, newest first
    public List<Incident> search(IncidentSearch search) {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(search::matches)
                .toList();
    }

    public Incident getForAdmin(Long incidentId) {
        return findIncident(incidentId);
    }

    // The admin picks a technician: for incidents nobody could take (CLASSIFIED), or to
    // reassign one that nobody acknowledged yet (ASSIGNED). After that it can't be "stolen".
    @Transactional
    public void assignManually(Long incidentId, Long technicianId, Admin admin) {
        Incident incident = findIncident(incidentId);
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new InvalidInputException("Unknown technician id " + technicianId + "."));
        if (!technician.isAvailable()) {
            throw new TechnicianUnavailableException(technician.getName() + " is marked as unavailable.");
        }
        if (assignmentService.workloadOf(technician) >= technician.getMaxActiveIncidents()) {
            throw new WorkloadExceededException(technician);
        }
        IncidentStatus old = incident.getStatus();
        if (old == IncidentStatus.ASSIGNED) {
            incident.reassignTo(technician); // e.g. after an escalation reached the admin
        } else {
            incident.assignTo(technician);
        }
        publish(incident, admin, EventType.ASSIGNED, old, IncidentStatus.ASSIGNED,
                "Assigned to " + technician.getName() + " by the admin.");
    }

    // Try the active strategy again, e.g. after a technician became available
    @Transactional
    public Technician retryAutoAssign(Long incidentId, Admin admin) {
        Incident incident = findIncident(incidentId);
        IncidentStatus old = incident.getStatus();
        Technician technician = assignmentService.assign(incident);
        publish(incident, admin, EventType.ASSIGNED, old, IncidentStatus.ASSIGNED,
                "Assigned to " + technician.getName() + " ("
                        + assignmentService.getActiveStrategy().getKey() + " strategy, started by the admin).");
        return technician;
    }

    // ----- Helpers -----

    // Admin changes the priority by hand, e.g. after learning the problem is worse than reported
    @Transactional
    public void changePriority(Long incidentId, Priority newPriority, String reason, Admin admin) {
        Incident incident = findIncident(incidentId);
        String why = requireText(reason, "The reason");
        Priority old = incident.getPriority();
        incident.changePriority(newPriority, slaService.findPolicy(incident.getService(), newPriority));
        publish(incident, admin, EventType.PRIORITY_CHANGED, incident.getStatus(), incident.getStatus(),
                "Priority " + old + " -> " + newPriority + ": " + why + ". Now resolve by "
                        + incident.getResolutionDueAt().toLocalTime().withNano(0) + ".");
    }

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
