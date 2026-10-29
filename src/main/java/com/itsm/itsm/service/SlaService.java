package com.itsm.itsm.service;

import com.itsm.itsm.exception.MissingSlaConfigException;
import com.itsm.itsm.model.ITService;
import com.itsm.itsm.model.Priority;
import com.itsm.itsm.model.SlaPolicy;
import com.itsm.itsm.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SlaService {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaService(SlaPolicyRepository slaPolicyRepository) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    // Use the service's own rule if it has one, otherwise the default rule
    public SlaPolicy findPolicy(ITService service, Priority priority) {
        return slaPolicyRepository.findByServiceAndPriority(service, priority)
                .or(() -> slaPolicyRepository.findByServiceIsNullAndPriority(priority))
                .orElseThrow(() -> new MissingSlaConfigException(priority));
    }

    public List<SlaPolicy> findAll() {
        return slaPolicyRepository.findAllByOrderByPriorityAsc();
    }
}
