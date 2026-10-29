package com.itsm.itsm.repository;

import com.itsm.itsm.model.ITService;
import com.itsm.itsm.model.Priority;
import com.itsm.itsm.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    // A special rule for this exact service
    Optional<SlaPolicy> findByServiceAndPriority(ITService service, Priority priority);

    // The default rule (no service)
    Optional<SlaPolicy> findByServiceIsNullAndPriority(Priority priority);

    List<SlaPolicy> findAllByOrderByPriorityAsc();
}
