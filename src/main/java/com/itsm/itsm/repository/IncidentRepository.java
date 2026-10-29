package com.itsm.itsm.repository;

import com.itsm.itsm.model.Employee;
import com.itsm.itsm.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByReporterOrderByCreatedAtDesc(Employee reporter);
}
