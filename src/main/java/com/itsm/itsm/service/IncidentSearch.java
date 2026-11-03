package com.itsm.itsm.service;

import com.itsm.itsm.model.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// The filters on the admin's "All incidents" page. Spring fills the fields from the
// URL, e.g. /admin/incidents?status=IN_PROGRESS&priority=P1. An empty field (null)
// means "don't filter on this". matches() says if one incident passes all filters.
public class IncidentSearch {

    private String number;          // "INC0003" or just "3"
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    private IncidentStatus status;
    private Priority priority;
    private Long categoryId;
    private Long serviceId;
    private Long technicianId;      // 0 = "not assigned yet"
    private SlaStatus slaStatus;

    public static final long UNASSIGNED = 0L;

    public boolean matches(Incident incident) {
        if (number != null && !number.isBlank() && !matchesNumber(incident)) {
            return false;
        }
        LocalDate created = incident.getCreatedAt().toLocalDate();
        if (from != null && created.isBefore(from)) {
            return false;
        }
        if (to != null && created.isAfter(to)) {
            return false;
        }
        if (status != null && incident.getStatus() != status) {
            return false;
        }
        if (priority != null && incident.getPriority() != priority) {
            return false;
        }
        if (categoryId != null && !incident.getCategory().getId().equals(categoryId)) {
            return false;
        }
        if (serviceId != null && !incident.getService().getId().equals(serviceId)) {
            return false;
        }
        if (technicianId != null && !matchesTechnician(incident)) {
            return false;
        }
        // SLA status is calculated from the clock, not stored, so it can only be filtered in Java
        if (slaStatus != null && incident.getSlaStatus() != slaStatus) {
            return false;
        }
        return true;
    }

    private boolean matchesNumber(Incident incident) {
        String digits = number.trim().toUpperCase().replace("INC", "");
        try {
            return incident.getId() != null && incident.getId() == Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return false; // not a number at all, so nothing matches
        }
    }

    private boolean matchesTechnician(Incident incident) {
        Technician assigned = incident.getAssignedTechnician();
        if (technicianId == UNASSIGNED) {
            return assigned == null;
        }
        return assigned != null && assigned.getId().equals(technicianId);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
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

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public SlaStatus getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(SlaStatus slaStatus) {
        this.slaStatus = slaStatus;
    }
}
