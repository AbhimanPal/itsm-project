package com.itsm.itsm.web;

import com.itsm.itsm.model.User;
import com.itsm.itsm.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // After login, send each user to their own home page
    @GetMapping("/")
    public String home(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return switch (user.getRole()) {
            case EMPLOYEE -> "redirect:/employee/incidents";
            case TECHNICIAN -> "redirect:/technician";
            case ADMIN -> "redirect:/admin";
        };
    }

    // Placeholder pages until the technician and admin screens are built
    @GetMapping("/technician")
    public String technicianHome() {
        return "technician/home";
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "admin/home";
    }
}
