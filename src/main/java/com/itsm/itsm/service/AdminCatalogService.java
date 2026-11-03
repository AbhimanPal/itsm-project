package com.itsm.itsm.service;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.ServiceNotFoundException;
import com.itsm.itsm.model.Category;
import com.itsm.itsm.model.ITService;
import com.itsm.itsm.model.Priority;
import com.itsm.itsm.model.SlaPolicy;
import com.itsm.itsm.repository.CategoryRepository;
import com.itsm.itsm.repository.ITServiceRepository;
import com.itsm.itsm.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Everything the admin can set up as data: IT services, categories and SLA policies.
// Every method checks its input first and throws InvalidInputException with a
// friendly message if something is wrong, so bad data never reaches the database.
@Service
public class AdminCatalogService {

    private final ITServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    public AdminCatalogService(ITServiceRepository serviceRepository, CategoryRepository categoryRepository,
                               SlaPolicyRepository slaPolicyRepository) {
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
    }

    // ----- IT services -----

    // id == null means "create a new one", otherwise "edit the existing one"
    @Transactional
    public ITService saveService(Long id, String name, int criticality, String description) {
        String cleanName = requireText(name, "Service name");
        if (criticality < 1 || criticality > 3) {
            throw new InvalidInputException("Criticality must be 1, 2 or 3.");
        }
        Optional<ITService> sameName = serviceRepository.findByNameIgnoreCase(cleanName);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            throw new InvalidInputException("A service called '" + cleanName + "' already exists.");
        }
        String cleanDescription = description == null ? "" : description.trim();

        if (id == null) {
            return serviceRepository.save(new ITService(cleanName, criticality, cleanDescription));
        }
        ITService service = serviceRepository.findById(id).orElseThrow(() -> new ServiceNotFoundException(id));
        service.update(cleanName, criticality, cleanDescription);
        return service;
    }

    // ----- Categories -----

    @Transactional
    public Category saveCategory(Long id, String name, String keywords) {
        String cleanName = requireText(name, "Category name");
        Optional<Category> sameName = categoryRepository.findByNameIgnoreCase(cleanName);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            throw new InvalidInputException("A category called '" + cleanName + "' already exists.");
        }
        String cleanKeywords = cleanKeywords(keywords);

        if (id == null) {
            return categoryRepository.save(new Category(cleanName, cleanKeywords));
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException("Unknown category id " + id + "."));
        category.update(cleanName, cleanKeywords);
        return category;
    }

    // " WiFi, Router ,," -> "wifi,router"
    private String cleanKeywords(String keywords) {
        if (keywords == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String word : keywords.split(",")) {
            String w = word.trim().toLowerCase();
            if (!w.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append(",");
                }
                result.append(w);
            }
        }
        return result.toString();
    }

    // ----- SLA policies -----

    // serviceId == null means the default rule for all services.
    // If a rule for this service + priority already exists, its times are changed;
    // otherwise a new rule is created.
    @Transactional
    public SlaPolicy saveSlaPolicy(Long serviceId, Priority priority, int responseMinutes, int resolutionMinutes) {
        if (priority == null) {
            throw new InvalidInputException("Choose a priority.");
        }
        if (responseMinutes <= 0 || resolutionMinutes <= 0) {
            throw new InvalidInputException("Times must be more than 0 minutes.");
        }
        if (responseMinutes > resolutionMinutes) {
            throw new InvalidInputException("The response time cannot be longer than the resolution time.");
        }

        Optional<SlaPolicy> existing;
        ITService service = null;
        if (serviceId == null) {
            existing = slaPolicyRepository.findByServiceIsNullAndPriority(priority);
        } else {
            service = serviceRepository.findById(serviceId).orElseThrow(() -> new ServiceNotFoundException(serviceId));
            existing = slaPolicyRepository.findByServiceAndPriority(service, priority);
        }

        if (existing.isPresent()) {
            existing.get().changeTimes(responseMinutes, resolutionMinutes);
            return existing.get();
        }
        return slaPolicyRepository.save(new SlaPolicy(service, priority, responseMinutes, resolutionMinutes));
    }

    // Only a service's special rule can be deleted. The default rules must stay,
    // otherwise new incidents would have no SLA (MissingSlaConfigException).
    @Transactional
    public void deleteSlaPolicy(Long policyId) {
        SlaPolicy policy = slaPolicyRepository.findById(policyId)
                .orElseThrow(() -> new InvalidInputException("Unknown SLA policy id " + policyId + "."));
        if (policy.isDefault()) {
            throw new InvalidInputException("Default SLA rules cannot be deleted, only changed.");
        }
        slaPolicyRepository.delete(policy);
    }

    private String requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException(what + " cannot be empty.");
        }
        return text.trim();
    }
}
