package com.example.ecommerce.common.kafka.publisher;

import com.example.ecommerce.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaSender<String, DomainEvent> kafkaSender;

    /**
     * 도메인 이벤트를 Kafka 토픽으로 발행
     *
     * @param topic 토픽 이름
     * @param event 도메인 이벤트
     * @return Mono<Void>
     */
    public Mono<Void> publish(String topic, DomainEvent event) {
        ProducerRecord<String, DomainEvent> record = new ProducerRecord<>(
                topic,
                event.getAggregateId(),
                event
        );

        SenderRecord<String, DomainEvent, String> senderRecord = SenderRecord.create(
                record,
                event.getEventId()
        );

        return kafkaSender.send(Mono.just(senderRecord))
                .doOnNext(result -> log.info("Event published: topic={}, eventId={}, offset={}",
                        topic, event.getEventId(), result.recordMetadata().offset()))
                .doOnError(error -> log.error("Failed to publish event: topic={}, eventId={}",
                        topic, event.getEventId(), error))
                .then();
    }

    /**
     * 키를 지정하여 이벤트 발행 (파티션 결정용)
     */
    public Mono<Void> publish(String topic, String key, DomainEvent event) {
        ProducerRecord<String, DomainEvent> record = new ProducerRecord<>(
                topic,
                key,
                event
        );

        SenderRecord<String, DomainEvent, String> senderRecord = SenderRecord.create(
                record,
                event.getEventId()
        );

        return kafkaSender.send(Mono.just(senderRecord))
                .doOnNext(result -> log.info("Event published: topic={}, key={}, eventId={}",
                        topic, key, event.getEventId()))
                .doOnError(error -> log.error("Failed to publish event: topic={}, eventId={}",
                        topic, event.getEventId(), error))
                .then();
    }
}
