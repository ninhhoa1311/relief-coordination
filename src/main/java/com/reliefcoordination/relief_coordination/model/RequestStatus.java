package com.reliefcoordination.relief_coordination.model;

import java.util.Set;
import java.util.Map;

public enum RequestStatus {
    SENT, ACCEPTED, PROCESSING, COMPLETED, CONFIRMED, CANCELLED;

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = Map.of(
        SENT, Set.of(ACCEPTED, CANCELLED),
        ACCEPTED, Set.of(PROCESSING, CANCELLED),
        PROCESSING, Set.of(COMPLETED, CANCELLED),
        COMPLETED, Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(),
        CANCELLED, Set.of()
    );

    public boolean canTransitionTo(RequestStatus next) {
        return ALLOWED_TRANSITIONS.get(this).contains(next);
    }
}