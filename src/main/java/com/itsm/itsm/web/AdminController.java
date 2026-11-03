package com.itsm.itsm.web;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.exception.ItsmException;
import com.itsm.itsm.model.*;
import com.itsm.itsm.report.ReportService;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import com.itsm.itsm.repository.UserRepository;
import com.itsm.itsm.service.IncidentSearch;
import com.itsm.itsm.service.IncidentService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AssignmentService assignmentService;
    private final AuditRecordRepository auditRecordRepository;
    private final ReportService reportService;
    private final IncidentService incidentService;
    private final CategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;

    public AdminController(AssignmentService assignmentService, AuditRecordRepository auditRecordRepository,
                           ReportService reportService, IncidentService incidentService,
                           CategoryRepository categoryRepository, ITServiceRepository serviceRepository,
                           TechnicianRepository technicianRepository, UserRepository userRepository) {
        this.assignmentService = assignmentService;
        this.auditRecordRepository = auditRecordRepository;
        this.reportService = reportService;
        this.incidentService = incidentService;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
    }

    // Dashboard: incident overview, technician workload, incidents per service
    @GetMapping
    public String dashboard(Model model) {
        List<Incident> incidents = reportService.allIncidents();
        model.addAttribute("total", incidents.size());
        model.addAttribute("open", reportService.countOpen(incidents));
        model.addAttribute("unassigned", reportService.countUnassigned(incidents));
        model.addAttribute("byStatus", reportService.countByStatus(incidents));
        model.addAttribute("byPriority", reportService.countByPriority(incidents));
        model.addAttribute("bySla", reportService.countBySlaStatus(incidents));
        model.addAttribute("workloads", reportService.technicianWorkloads(incidents));
        model.addAttribute("serviceReports", reportService.serviceReports(incidents));
        model.addAttribute("strategies", assignmentService.getStrategies());
        model.addAttribute("activeStrategy", assignmentService.getActiveStrategy().getKey());
        return "admin/home";
    }

    @PostMapping("/assignment-strategy")
    public String changeStrategy(@RequestParam String strategy, RedirectAttributes redirect) {
        assignmentService.setActiveStrategy(strategy);
        redirect.addFlashAttribute("message", "Assignment strategy changed.");
        return "redirect:/admin";
    }

    // ----- All incidents, with filters -----

    // Spring fills "search" from the URL's ?status=...&priority=... values
    @GetMapping("/incidents")
    public String incidents(@ModelAttribute("search") IncidentSearch search, Model model) {
        model.addAttribute("incidents", incidentService.search(search));
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("slaStatuses", SlaStatus.values());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("services", serviceRepository.findAllByOrderByNameAsc());
        model.addAttribute("technicians", technicianRepository.findAll(Sort.by("name")));
        return "admin/incidents";
    }

    @GetMapping("/incidents/{id}")
    public String incident(@PathVariable Long id, Model model) {
        Incident incident = incidentService.getForAdmin(id);
        model.addAttribute("incident", incident);
        model.addAttribute("history", auditRecordRepository.findByIncidentOrderByTimestampAscIdAsc(incident));
        model.addAttribute("technicians", technicianRepository.findAll(Sort.by("name")));
        model.addAttribute("duplicates", incidentService.findDuplicatesOf(incident));
        return "admin/incident";
    }

    @PostMapping("/incidents/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long technicianId, Principal principal,
                         RedirectAttributes redirect) {
        try {
            incidentService.assignManually(id, technicianId, currentAdmin(principal));
            redirect.addFlashAttribute("message", "Technician assigned.");
        } catch (ItsmException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/auto-assign")
    public String autoAssign(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            Technician technician = incidentService.retryAutoAssign(id, currentAdmin(principal));
            redirect.addFlashAttribute("message", "Assigned to " + technician.getName() + ".");
        } catch (ItsmException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/incidents/" + id;
    }

    // ----- Audit log -----

    @GetMapping("/audit")
    public String auditLog(Model model) {
        model.addAttribute("records", auditRecordRepository.findAllByOrderByTimestampDescIdDesc());
        return "admin/audit";
    }

    private Admin currentAdmin(Principal principal) {
        return (Admin) userRepository.findByEmail(principal.getName()).orElseThrow();
    }
}
