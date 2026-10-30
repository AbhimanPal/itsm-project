package com.itsm.itsm.web;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.Technician;
import com.itsm.itsm.repository.TechnicianRepository;
import com.itsm.itsm.service.IncidentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/technician")
public class TechnicianController {

    private final IncidentService incidentService;
    private final AssignmentService assignmentService;
    private final TechnicianRepository technicianRepository;
    private final AuditRecordRepository auditRecordRepository;

    public TechnicianController(IncidentService incidentService, AssignmentService assignmentService,
                                TechnicianRepository technicianRepository,
                                AuditRecordRepository auditRecordRepository) {
        this.incidentService = incidentService;
        this.assignmentService = assignmentService;
        this.technicianRepository = technicianRepository;
        this.auditRecordRepository = auditRecordRepository;
    }

    @GetMapping
    public String myWork(Principal principal, Model model) {
        Technician technician = currentTechnician(principal);
        List<Incident> incidents = incidentService.findByTechnician(technician);
        model.addAttribute("technician", technician);
        model.addAttribute("incidents", incidents);
        model.addAttribute("workload", assignmentService.workloadOf(technician));
        return "technician/home";
    }

    @GetMapping("/incidents/{id}")
    public String incident(@PathVariable Long id, Principal principal, Model model) {
        Incident incident = incidentService.getForTechnician(id, currentTechnician(principal));
        model.addAttribute("incident", incident);
        model.addAttribute("history", auditRecordRepository.findByIncidentOrderByTimestampAscIdAsc(incident));
        return "technician/incident";
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public String acknowledge(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        incidentService.acknowledge(id, currentTechnician(principal));
        return back(id, redirect, "Incident acknowledged.");
    }

    @PostMapping("/incidents/{id}/start")
    public String start(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        incidentService.startWork(id, currentTechnician(principal));
        return back(id, redirect, "Work started.");
    }

    @PostMapping("/incidents/{id}/note")
    public String note(@PathVariable Long id, @RequestParam String text, Principal principal,
                       RedirectAttributes redirect) {
        incidentService.addNote(id, currentTechnician(principal), text);
        return back(id, redirect, "Note added.");
    }

    @PostMapping("/incidents/{id}/ask")
    public String ask(@PathVariable Long id, @RequestParam String question, Principal principal,
                      RedirectAttributes redirect) {
        incidentService.askForInfo(id, currentTechnician(principal), question);
        return back(id, redirect, "Question sent to the reporter.");
    }

    @PostMapping("/incidents/{id}/resume")
    public String resume(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        incidentService.resumeWork(id, currentTechnician(principal));
        return back(id, redirect, "Work resumed.");
    }

    @PostMapping("/incidents/{id}/resolve")
    public String resolve(@PathVariable Long id, @RequestParam String resolution, Principal principal,
                          RedirectAttributes redirect) {
        incidentService.resolve(id, currentTechnician(principal), resolution);
        return back(id, redirect, "Incident resolved.");
    }

    private String back(Long id, RedirectAttributes redirect, String message) {
        redirect.addFlashAttribute("message", message);
        return "redirect:/technician/incidents/" + id;
    }

    private Technician currentTechnician(Principal principal) {
        return technicianRepository.findByEmail(principal.getName()).orElseThrow();
    }
}
