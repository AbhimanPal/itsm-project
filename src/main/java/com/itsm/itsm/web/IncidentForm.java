package com.itsm.itsm.web;

import jakarta.validation.constraints.*;

public class IncidentForm {

    @NotBlank(message = "Give the problem a short title")
    @Size(max = 100, message = "Keep the title under 100 characters")
    private String title;

    @NotBlank(message = "Describe the problem")
    @Size(max = 2000, message = "Keep the description under 2000 characters")
    private String description;

    @NotNull(message = "Choose a category")
    private Long categoryId;

    @NotNull(message = "Choose the affected service")
    private Long serviceId;

    @Min(1) @Max(3)
    private int impact = 2;

    @Min(1) @Max(3)
    private int urgency = 2;

    @Min(value = 1, message = "At least 1 user must be affected")
    private int usersAffected = 1;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public int getImpact() {
        return impact;
    }

    public void setImpact(int impact) {
        this.impact = impact;
    }

    public int getUrgency() {
        return urgency;
    }

    public void setUrgency(int urgency) {
        this.urgency = urgency;
    }

    public int getUsersAffected() {
        return usersAffected;
    }

    public void setUsersAffected(int usersAffected) {
        this.usersAffected = usersAffected;
    }
}
