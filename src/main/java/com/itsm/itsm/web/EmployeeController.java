package com.itsm.itsm.web;

import com.itsm.itsm.exception.InvalidLoginException;
import com.itsm.itsm.exception.UnauthorizedOperationException;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.exception.DuplicateIncidentException;
import com.itsm.itsm.model.Employee;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.EmployeeRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private final IncidentService incidentService;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final AuditRecordRepository auditRecordRepository;

    public EmployeeController(IncidentService incidentService, EmployeeRepository employeeRepository,
                              CategoryRepository categoryRepository, ITServiceRepository serviceRepository,
                              AuditRecordRepository auditRecordRepository) {
        this.incidentService = incidentService;
        this.employeeRepository = employeeRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.auditRecordRepository = auditRecordRepository;
    }

    @GetMapping("/incidents")
    public String myIncidents(Principal principal, Model model) {
        model.addAttribute("incidents", incidentService.findByReporter(currentEmployee(principal)));
        return "employee/incidents";
    }

    @GetMapping("/incidents/new")
    public String newIncidentForm(Model model) {
        model.addAttribute("form", new IncidentForm());
        addDropdowns(model);
        return "employee/new-incident";
    }

    @PostMapping("/incidents")
    public String reportIncident(@Valid @ModelAttribute("form") IncidentForm form, BindingResult result,
                                 Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            addDropdowns(model);
            return "employee/new-incident";
        }
        Incident incident;
        try {
            incident = incidentService.reportIncident(currentEmployee(principal), form.getTitle(),
                    form.getDescription(), form.getCategoryId(), form.getServiceId(),
                    form.getImpact(), form.getUrgency(), form.getUsersAffected());
        } catch (DuplicateIncidentException e) {
            // Show the message at the top of the form instead of an error page
            result.reject("duplicate", e.getMessage());
            addDropdowns(model);
            return "employee/new-incident";
        }

        String message = incident.getNumber() + " was reported with priority "
                + incident.getPriority() + " (" + incident.getPriority().getLabel() + ").";
        if (incident.getDuplicateOf() != null) {
            message += " It looks like " + incident.getDuplicateOf().getNumber()
                    + ", which is already being handled, so we linked them.";
        }
        redirect.addFlashAttribute("message", message);
        return "redirect:/employee/incidents";
    }

    @GetMapping("/incidents/{id}")
    public String incident(@PathVariable Long id, Principal principal, Model model) {
        Incident incident = incidentService.getForReporter(id, currentEmployee(principal));
        model.addAttribute("incident", incident);
        model.addAttribute("history", auditRecordRepository.findByIncidentOrderByTimestampAscIdAsc(incident));
        return "employee/incident";
    }

    @PostMapping("/incidents/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String text, Principal principal,
                        RedirectAttributes redirect) {
        incidentService.reply(id, currentEmployee(principal), text);
        return back(id, redirect, "Reply sent to the technician.");
    }

    @PostMapping("/incidents/{id}/confirm")
    public String confirm(@PathVariable Long id, @RequestParam int rating,
                          @RequestParam(required = false) String comment, Principal principal,
                          RedirectAttributes redirect) {
        incidentService.confirmFix(id, currentEmployee(principal), rating, comment);
        return back(id, redirect, "Thanks! The incident is closed.");
    }

    @PostMapping("/incidents/{id}/reopen")
    public String reopen(@PathVariable Long id, @RequestParam String reason, Principal principal,
                         RedirectAttributes redirect) {
        incidentService.reopen(id, currentEmployee(principal), reason);
        return back(id, redirect, "The incident was reopened and sent back to the technician.");
    }

    private String back(Long id, RedirectAttributes redirect, String message) {
        redirect.addFlashAttribute("message", message);
        return "redirect:/employee/incidents/" + id;
    }

    private Employee currentEmployee(Principal principal) {
        Employee employee = employeeRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new InvalidLoginException(principal.getName()));
        // Deactivated while still logged in: stop them at the next click
        if (!employee.isActive()) {
            throw new UnauthorizedOperationException("Your account has been deactivated. Please contact the IT admin.");
        }
        return employee;
    }

    private void addDropdowns(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("services", serviceRepository.findAllByOrderByNameAsc());
    }
}
