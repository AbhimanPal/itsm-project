package com.itsm.itsm.repository;

import com.itsm.itsm.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByEmail(String email);

    List<Technician> findByTeamLeadTrueAndAvailableTrue();
}
