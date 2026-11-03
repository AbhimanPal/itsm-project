package com.itsm.itsm.model;

import jakarta.persistence.*;

// One row = "for this service and this priority, respond within X minutes
// and resolve within Y minutes". If service is null, the row is the default
// for every service that has no special rule.
@Entity
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ITService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private int responseMinutes;

    private int resolutionMinutes;

    protected SlaPolicy() {
    }

    public SlaPolicy(ITService service, Priority priority, int responseMinutes, int resolutionMinutes) {
        this.service = service;
        this.priority = priority;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public Long getId() {
        return id;
    }

    public ITService getService() {
        return service;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }

    // Only the times can change; service + priority identify the rule
    public void changeTimes(int responseMinutes, int resolutionMinutes) {
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public boolean isDefault() {
        return service == null;
    }
}
