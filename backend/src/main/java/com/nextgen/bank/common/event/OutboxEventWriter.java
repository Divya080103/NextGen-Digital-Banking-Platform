package com.nextgen.bank.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OutboxEventWriter {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Inserts an outbox event within the currently active database transaction.
     * Guaranteed to commit or rollback atomically alongside core domain entity changes.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent write(DomainEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event.getPayload());
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .eventType(event.getEventType())
                    .payloadJson(payloadJson)
                    .status("PENDING")
                    .createdAt(event.getOccurredOn() != null ? event.getOccurredOn() : Instant.now())
                    .retryCount(0)
                    .build();
            return repository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event payload to JSON", e);
        }
    }

    /**
     * Overloaded helper method for ad-hoc event publishing.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent write(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payloadJson(payloadJson)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();
            return repository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event payload to JSON", e);
        }
    }
}
