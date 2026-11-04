package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Category;
import com.itsm.itsm.model.Employee;
import com.itsm.itsm.model.Technician;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.EmployeeRepository;
import com.itsm.itsm.repository.TechnicianRepository;
import com.itsm.itsm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The admin creates technicians and changes their skills, availability and workload limit.
// Employees register themselves; the admin can correct their details and deactivate them.
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;

    public AdminUserService(UserRepository userRepository, TechnicianRepository technicianRepository,
                            CategoryRepository categoryRepository, PasswordEncoder passwordEncoder,
                            EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
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

    // ----- Employees -----

    public Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException("Unknown employee id " + id + "."));
    }

    @Transactional
    public Employee updateEmployee(Long id, String name, String department) {
        if (name == null || name.isBlank() || department == null || department.isBlank()) {
            throw new InvalidInputException("Name and department cannot be empty.");
        }
        Employee employee = findEmployee(id);
        employee.updateDetails(name.trim(), department.trim());
        return employee;
    }

    // Deactivate instead of delete: the employee's incidents and history must stay complete
    @Transactional
    public Employee setEmployeeActive(Long id, boolean active) {
        Employee employee = findEmployee(id);
        if (active) {
            employee.reactivate();
        } else {
            employee.deactivate();
        }
        return employee;
    }

    private Set<Category> loadCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new InvalidInputException("A technician needs at least one skill.");
        }
        return new HashSet<>(categoryRepository.findAllById(ids));
    }
}
