package com.itsm.itsm.repository;

import com.itsm.itsm.model.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ITServiceRepository extends JpaRepository<ITService, Long> {

    List<ITService> findAllByOrderByNameAsc();

    Optional<ITService> findByName(String name);

    Optional<ITService> findByNameIgnoreCase(String name);
}
