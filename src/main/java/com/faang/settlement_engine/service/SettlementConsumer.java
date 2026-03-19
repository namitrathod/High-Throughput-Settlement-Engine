package com.faang.settlement_engine.service;

import com.faang.settlement_engine.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class SettlementConsumer {
    private static final String TOPIC_SETTLEMENT_EVENTS = "settlement-events";
    private final Random random = new Random();

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.ALWAYS_RETRY_ON_ERROR
    )
    @KafkaListener(topics = TOPIC_SETTLEMENT_EVENTS, groupId = "settlement-group")
    public void consumeSettlementEvent(TransactionEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Consuming event from topic {}: {}", topic, event);

        // Simulate external bank API call
        simulateExternalBankSettlement(event);

        log.info("External settlement completed for key: {}", event.getIdempotencyKey());
    }

    private void simulateExternalBankSettlement(TransactionEvent event) {
        // Simulate a failure 20% of the time to test DLQ/Retry
        if (random.nextInt(100) < 20) {
            log.warn("Simulated transient failure for settlement key: {}", event.getIdempotencyKey());
            throw new RuntimeException("External Bank API Unavailable");
        }
        
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @DltHandler
    public void handleDlt(TransactionEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("Event moved to DLQ: topic={}, event={}", topic, event);
        // Here you would typically store the failed transaction in a DB table for manual intervention.
    }
}
