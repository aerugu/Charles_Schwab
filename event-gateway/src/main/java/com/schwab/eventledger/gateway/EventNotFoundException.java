package com.schwab.eventledger.gateway;

class EventNotFoundException extends RuntimeException {
    EventNotFoundException(String eventId) {
        super("Event not found: " + eventId);
    }
}
