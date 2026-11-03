package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Category;
import com.itsm.itsm.model.Technician;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import com.itsm.itsm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The admin creates technicians and changes their skills, availability and workload limit.
// (Employees register themselves on the register page.)
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, TechnicianRepository technicianRepository,
                            CategoryRepository categoryRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email.trim());
    }

    @Transactional
    public Technician createTechnician(String name, String email, String password, int maxActiveIncidents,
                                       boolean teamLead, List<Long> skillIds) {
        String cleanEmail = email.trim();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new InvalidInputException("An account with this email already exists.");
        }
        Technician technician = new Technician(name.trim(), cleanEmail, passwordEncoder.encode(password),
                maxActiveIncidents, teamLead);
        technician.replaceSkills(loadCategories(skillIds));
        return technicianRepository.save(technician);
    }

    @Transactional
    public Technician updateTechnician(Long id, int maxActiveIncidents, boolean available, boolean teamLead,
                                       List<Long> skillIds) {
        if (maxActiveIncidents < 1 || maxActiveIncidents > 20) {
            throw new InvalidInputException("Max active incidents must be between 1 and 20.");
        }
        Technician technician = findTechnician(id);
        technician.updateSettings(maxActiveIncidents, available, teamLead);
        technician.replaceSkills(loadCategories(skillIds));
        return technician;
    }

    public Technician findTechnician(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException("Unknown technician id " + id + "."));
    }

    private Set<Category> loadCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new InvalidInputException("A technician needs at least one skill.");
        }
        return new HashSet<>(categoryRepository.findAllById(ids));
    }
}
