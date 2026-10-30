package com.itsm.itsm.web;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.audit.AuditRecordRepository;
import com.itsm.itsm.service.SlaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AssignmentService assignmentService;
    private final SlaService slaService;
    private final AuditRecordRepository auditRecordRepository;

    public AdminController(AssignmentService assignmentService, SlaService slaService,
                           AuditRecordRepository auditRecordRepository) {
        this.assignmentService = assignmentService;
        this.slaService = slaService;
        this.auditRecordRepository = auditRecordRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("strategies", assignmentService.getStrategies());
        model.addAttribute("activeStrategy", assignmentService.getActiveStrategy().getKey());
        model.addAttribute("slaPolicies", slaService.findAll());
        return "admin/home";
    }

    @GetMapping("/audit")
    public String auditLog(Model model) {
        model.addAttribute("records", auditRecordRepository.findAllByOrderByTimestampDescIdDesc());
        return "admin/audit";
    }

    @PostMapping("/assignment-strategy")
    public String changeStrategy(@RequestParam String strategy, RedirectAttributes redirect) {
        assignmentService.setActiveStrategy(strategy);
        redirect.addFlashAttribute("message", "Assignment strategy changed.");
        return "redirect:/admin";
    }
}
