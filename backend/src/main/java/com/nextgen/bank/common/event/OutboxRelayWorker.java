package com.nextgen.bank.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public OutboxRelayWorker(OutboxEventRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${app.outbox.relay.cron:*/5 * * * * *}")
    @Transactional
    public void processPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = repository.findPendingEvents(PageRequest.of(0, BATCH_SIZE));
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("OutboxRelayWorker polling found {} pending events to process.", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish event internally via Spring ApplicationEventPublisher
                eventPublisher.publishEvent(event);

                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
                repository.save(event);
                log.info("Outbox event [{}] of type [{}] marked PROCESSED.", event.getEventId(), event.getEventType());
            } catch (Exception ex) {
                log.error("Failed to relay outbox event [{}]: {}", event.getEventId(), ex.getMessage(), ex);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus("FAILED");
                }
                repository.save(event);
            }
        }
    }
}
