package com.itsm.itsm.repository;

import com.itsm.itsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository pattern: Spring Data writes the SQL for us from the method names.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
