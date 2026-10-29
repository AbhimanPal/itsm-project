package com.itsm.itsm.model;

import jakarta.persistence.*;

// A category is data (a table row), not an enum, so the admin can add
// new categories without changing any code.
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Comma-separated words used later for auto-categorization, e.g. "wifi,router,vpn"
    private String keywords;

    protected Category() {
    }

    public Category(String name, String keywords) {
        this.name = name;
        this.keywords = keywords;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKeywords() {
        return keywords;
    }
}
