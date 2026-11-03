package com.itsm.itsm.web;

import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

// What the admin types in the "Add technician" form. Like RegisterForm, the
// annotations are validation rules that Spring checks before our code runs.
public class TechnicianForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Min(value = 1, message = "At least 1")
    @Max(value = 20, message = "At most 20")
    private int maxActiveIncidents = 5;

    private boolean teamLead;

    // The ids of the ticked skill checkboxes
    private List<Long> skillIds = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxActiveIncidents() {
        return maxActiveIncidents;
    }

    public void setMaxActiveIncidents(int maxActiveIncidents) {
        this.maxActiveIncidents = maxActiveIncidents;
    }

    public boolean isTeamLead() {
        return teamLead;
    }

    public void setTeamLead(boolean teamLead) {
        this.teamLead = teamLead;
    }

    public List<Long> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<Long> skillIds) {
        this.skillIds = skillIds;
    }
}
