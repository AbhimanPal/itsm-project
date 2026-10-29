package com.itsm.itsm.repository;

import com.itsm.itsm.model.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ITServiceRepository extends JpaRepository<ITService, Long> {

    List<ITService> findAllByOrderByNameAsc();
}
