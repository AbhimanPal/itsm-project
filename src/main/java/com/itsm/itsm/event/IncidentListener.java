package com.itsm.itsm.event;

// Observer pattern: anything that wants to react to incident events
// (audit trail, notifications, ...) implements this interface.
public interface IncidentListener {

    void onEvent(IncidentEvent event);
}
