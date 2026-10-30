package com.itsm.itsm.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidentEventPublisherTest {

    // A fake listener that just remembers what it was told
    static class RecordingListener implements IncidentListener {
        final List<IncidentEvent> received = new ArrayList<>();

        @Override
        public void onEvent(IncidentEvent event) {
            received.add(event);
        }
    }

    @Test
    void everyListenerReceivesEveryEvent() {
        RecordingListener first = new RecordingListener();
        RecordingListener second = new RecordingListener();
        IncidentEventPublisher publisher = new IncidentEventPublisher(List.of(first, second));

        IncidentEvent event = new IncidentEvent(null, null, EventType.CREATED, null, null, "test");
        publisher.publish(event);

        assertEquals(List.of(event), first.received);
        assertEquals(List.of(event), second.received);
    }
}
