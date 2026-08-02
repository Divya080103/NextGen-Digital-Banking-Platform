package com.nextgen.bank.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID getEventId();
    String getAggregateType();
    String getAggregateId();
    String getEventType();
    Instant getOccurredOn();
    Object getPayload();
}
