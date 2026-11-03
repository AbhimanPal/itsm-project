package com.itsm.itsm.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    // A Set, because a technician has each skill at most once
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "technician_skills")
    private Set<Category> skills = new HashSet<>();

    private int maxActiveIncidents;

    private boolean available = true;

    private boolean teamLead;

    protected Technician() {
    }

    public Technician(String name, String email, String passwordHash, int maxActiveIncidents, boolean teamLead) {
        super(name, email, passwordHash);
        this.maxActiveIncidents = maxActiveIncidents;
        this.teamLead = teamLead;
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    // Replace all skills at once (used by the admin's edit form)
    public void replaceSkills(Set<Category> newSkills) {
        skills.clear();
        skills.addAll(newSkills);
    }

    public void updateSettings(int maxActiveIncidents, boolean available, boolean teamLead) {
        this.maxActiveIncidents = maxActiveIncidents;
        this.available = available;
        this.teamLead = teamLead;
    }

    public void addSkill(Category category) {
        skills.add(category);
    }

    public boolean hasSkill(Category category) {
        for (Category skill : skills) {
            if (skill.getName().equals(category.getName())) {
                return true;
            }
        }
        return false;
    }

    public Set<Category> getSkills() {
        return skills;
    }

    public int getMaxActiveIncidents() {
        return maxActiveIncidents;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isTeamLead() {
        return teamLead;
    }
}
