package com.itsm.itsm.event;

import org.springframework.stereotype.Component;

import java.util.List;

// The "subject" in the Observer pattern. It doesn't know what the listeners do;
// it just tells all of them. Spring finds every IncidentListener class for us.
@Component
public class IncidentEventPublisher {

    private final List<IncidentListener> listeners;

    public IncidentEventPublisher(List<IncidentListener> listeners) {
        this.listeners = listeners;
    }

    public void publish(IncidentEvent event) {
        for (IncidentListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
