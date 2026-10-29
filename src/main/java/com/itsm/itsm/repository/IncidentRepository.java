package com.itsm.itsm.repository;

import com.itsm.itsm.model.Employee;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByReporterOrderByCreatedAtDesc(Employee reporter);

    List<Incident> findByAssignedTechnicianOrderByCreatedAtDesc(Technician technician);

    // Workload: how many incidents this technician is working on right now
    long countByAssignedTechnicianAndStatusIn(Technician technician, Collection<IncidentStatus> statuses);
}
