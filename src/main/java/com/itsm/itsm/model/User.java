package com.itsm.itsm.model;

import jakarta.persistence.*;

// Abstraction + inheritance: there is no plain "User" in the system, only
// Employees, Technicians and Admins. All three are stored in one "users" table;
// the user_type column says which subclass each row is.
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Encapsulation: we only ever store the BCrypt hash, never the real password
    @Column(nullable = false)
    private String passwordHash;

    // JPA needs an empty constructor to load rows from the database
    protected User() {
    }

    protected User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Each subclass must say which role it is (polymorphism)
    public abstract Role getRole();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
