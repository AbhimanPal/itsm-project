package com.itsm.itsm.web;

import com.itsm.itsm.assignment.AssignmentService;
import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Technician;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import com.itsm.itsm.repository.UserRepository;
import com.itsm.itsm.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService userService;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final CategoryRepository categoryRepository;
    private final AssignmentService assignmentService;

    public AdminUserController(AdminUserService userService, UserRepository userRepository,
                               TechnicianRepository technicianRepository, CategoryRepository categoryRepository,
                               AssignmentService assignmentService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
        this.categoryRepository = categoryRepository;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public String users(Model model) {
        List<Technician> technicians = technicianRepository.findAll(Sort.by("name"));
        // technician id -> number of incidents they are working on now
        Map<Long, Long> workload = new HashMap<>();
        for (Technician t : technicians) {
            workload.put(t.getId(), assignmentService.workloadOf(t));
        }
        model.addAttribute("technicians", technicians);
        model.addAttribute("workload", workload);
        model.addAttribute("users", userRepository.findAll(Sort.by("name")));
        return "admin/users";
    }

    @GetMapping("/technicians/new")
    public String newTechnicianForm(Model model) {
        model.addAttribute("form", new TechnicianForm());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        return "admin/new-technician";
    }

    @PostMapping("/technicians")
    public String createTechnician(@Valid @ModelAttribute("form") TechnicianForm form, BindingResult result,
                                   Model model, RedirectAttributes redirect) {
        if (form.getEmail() != null && userService.emailTaken(form.getEmail())) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
        }
        if (form.getSkillIds().isEmpty()) {
            result.rejectValue("skillIds", "empty", "Tick at least one skill");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
            return "admin/new-technician";
        }
        Technician technician = userService.createTechnician(form.getName(), form.getEmail(), form.getPassword(),
                form.getMaxActiveIncidents(), form.isTeamLead(), form.getSkillIds());
        redirect.addFlashAttribute("message", "Technician " + technician.getName() + " created.");
        return "redirect:/admin/users";
    }

    @GetMapping("/technicians/{id}")
    public String editTechnicianForm(@PathVariable Long id, Model model) {
        Technician technician = userService.findTechnician(id);
        model.addAttribute("technician", technician);
        model.addAttribute("workload", assignmentService.workloadOf(technician));
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        return "admin/edit-technician";
    }

    // An unticked checkbox sends nothing, so "available" and "teamLead" default to false
    @PostMapping("/technicians/{id}")
    public String updateTechnician(@PathVariable Long id, @RequestParam int maxActiveIncidents,
                                   @RequestParam(defaultValue = "false") boolean available,
                                   @RequestParam(defaultValue = "false") boolean teamLead,
                                   @RequestParam(required = false) List<Long> skillIds,
                                   RedirectAttributes redirect) {
        try {
            Technician technician = userService.updateTechnician(id, maxActiveIncidents, available, teamLead, skillIds);
            redirect.addFlashAttribute("message", technician.getName() + " updated.");
            return "redirect:/admin/users";
        } catch (InvalidInputException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/technicians/" + id;
        }
    }
}
