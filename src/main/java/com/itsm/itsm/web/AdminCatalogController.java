package com.itsm.itsm.web;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Priority;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.service.AdminCatalogService;
import com.itsm.itsm.service.SlaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Admin pages for the data that drives the system: IT services, categories, SLA policies.
// Each page shows a table plus one form. "?edit=5" fills the form with row 5 so it can be changed.
@Controller
@RequestMapping("/admin")
public class AdminCatalogController {

    private final AdminCatalogService catalogService;
    private final ITServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;
    private final SlaService slaService;

    public AdminCatalogController(AdminCatalogService catalogService, ITServiceRepository serviceRepository,
                                  CategoryRepository categoryRepository, SlaService slaService) {
        this.catalogService = catalogService;
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
        this.slaService = slaService;
    }

    // ----- IT services -----

    @GetMapping("/services")
    public String services(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("services", serviceRepository.findAllByOrderByNameAsc());
        model.addAttribute("editing", edit == null ? null : serviceRepository.findById(edit).orElse(null));
        return "admin/services";
    }

    @PostMapping("/services")
    public String saveService(@RequestParam(required = false) Long id, @RequestParam String name,
                              @RequestParam int criticality, @RequestParam(required = false) String description,
                              RedirectAttributes redirect) {
        try {
            catalogService.saveService(id, name, criticality, description);
            redirect.addFlashAttribute("message", "Service '" + name.trim() + "' saved.");
        } catch (InvalidInputException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/services";
    }

    // ----- Categories -----

    @GetMapping("/categories")
    public String categories(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("editing", edit == null ? null : categoryRepository.findById(edit).orElse(null));
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String saveCategory(@RequestParam(required = false) Long id, @RequestParam String name,
                               @RequestParam(required = false) String keywords, RedirectAttributes redirect) {
        try {
            catalogService.saveCategory(id, name, keywords);
            redirect.addFlashAttribute("message", "Category '" + name.trim() + "' saved.");
        } catch (InvalidInputException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ----- SLA policies -----

    @GetMapping("/sla")
    public String slaPolicies(Model model) {
        model.addAttribute("slaPolicies", slaService.findAll());
        model.addAttribute("services", serviceRepository.findAllByOrderByNameAsc());
        model.addAttribute("priorities", Priority.values());
        return "admin/sla";
    }

    // Integer (not int) so an empty box arrives as null instead of crashing the page
    @PostMapping("/sla")
    public String saveSlaPolicy(@RequestParam(required = false) Long serviceId, @RequestParam Priority priority,
                                @RequestParam(required = false) Integer responseMinutes,
                                @RequestParam(required = false) Integer resolutionMinutes,
                                RedirectAttributes redirect) {
        try {
            catalogService.saveSlaPolicy(serviceId, priority,
                    responseMinutes == null ? 0 : responseMinutes,
                    resolutionMinutes == null ? 0 : resolutionMinutes);
            redirect.addFlashAttribute("message", "SLA policy saved. It applies to incidents reported from now on.");
        } catch (InvalidInputException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sla";
    }

    @PostMapping("/sla/{id}/delete")
    public String deleteSlaPolicy(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            catalogService.deleteSlaPolicy(id);
            redirect.addFlashAttribute("message", "SLA policy deleted. That service now uses the default rule.");
        } catch (InvalidInputException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sla";
    }
}
