package com.itsm.itsm.event;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.model.User;

// "Something happened to an incident." Created by IncidentService and handed
// to every listener. actor is null when the system itself did it.
public class IncidentEvent {

    private final Incident incident;
    private final User actor;
    private final EventType type;
    private final IncidentStatus oldStatus;
    private final IncidentStatus newStatus;
    private final String description;

    public IncidentEvent(Incident incident, User actor, EventType type,
                         IncidentStatus oldStatus, IncidentStatus newStatus, String description) {
        this.incident = incident;
        this.actor = actor;
        this.type = type;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.description = description;
    }

    public Incident getIncident() {
        return incident;
    }

    public User getActor() {
        return actor;
    }

    public EventType getType() {
        return type;
    }

    public IncidentStatus getOldStatus() {
        return oldStatus;
    }

    public IncidentStatus getNewStatus() {
        return newStatus;
    }

    public String getDescription() {
        return description;
    }
}
