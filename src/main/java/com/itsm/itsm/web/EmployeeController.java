package com.itsm.itsm.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private final IncidentService incidentService;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;

    public EmployeeController(IncidentService incidentService, EmployeeRepository employeeRepository,
                              CategoryRepository categoryRepository, ITServiceRepository serviceRepository) {
        this.incidentService = incidentService;
        this.employeeRepository = employeeRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
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
        Incident incident = incidentService.reportIncident(currentEmployee(principal), form.getTitle(),
                form.getDescription(), form.getCategoryId(), form.getServiceId(),
                form.getImpact(), form.getUrgency(), form.getUsersAffected());

        redirect.addFlashAttribute("message", incident.getNumber() + " was reported with priority "
                + incident.getPriority() + " (" + incident.getPriority().getLabel() + ").");
        return "redirect:/employee/incidents";
    }

    private Employee currentEmployee(Principal principal) {
        return employeeRepository.findByEmail(principal.getName()).orElseThrow();
    }

    private void addDropdowns(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("services", serviceRepository.findAllByOrderByNameAsc());
    }
}
