package com.itsm.itsm.model;

import jakarta.persistence.*;

@Entity
@Table(name = "it_service")
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // How important this service is to the business: 1 = low, 3 = high.
    // Set by the admin and used in the priority formula.
    private int criticality;

    private String description;

    protected ITService() {
    }

    public ITService(String name, int criticality, String description) {
        this.name = name;
        this.criticality = criticality;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCriticality() {
        return criticality;
    }

    public String getDescription() {
        return description;
    }
}
