package com.andesstay.reservations.service;

import com.andesstay.reservations.config.KafkaProducerConfig;
import com.andesstay.reservations.dto.AnalyticsTelemetryEvent;
import com.andesstay.reservations.dto.ReservationStreamEvent;
import com.andesstay.reservations.entity.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaReservationStreamProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaReservationStreamProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaReservationStreamProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Emite un evento tipado de streaming al crear o confirmar una reserva (Tópico 1).
     */
    public void publishReservationEvent(Reservation reservation, String eventType) {
        ReservationStreamEvent event = ReservationStreamEvent.of(
                eventType,
                reservation.getId(),
                reservation.getHuesped(),
                reservation.getUnidad(),
                reservation.getEstado(),
                85000.0,
                2
        );

        String key = String.valueOf(reservation.getId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_RESERVATIONS_STREAM, key, event);

        if (future != null) {
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[Kafka Producer] Evento '{}' emitido exitosamente a tópico '{}' [Partición: {}, Offset: {}, EventId: {}]",
                            eventType,
                            KafkaProducerConfig.TOPIC_RESERVATIONS_STREAM,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.getEventId());
                } else {
                    log.error("[Kafka Producer] Fallo al emitir evento a Kafka: {}", ex.getMessage());
                }
            });
        }
    }

    /**
     * Emite telemetría y analítica de ocupación al Tópico 2 de Kafka.
     */
    public void publishTelemetryMetric(String unitName, Double occupancyRate, int activeCount) {
        AnalyticsTelemetryEvent metric = AnalyticsTelemetryEvent.of(
                "OCCUPANCY_TELEMETRY",
                unitName,
                occupancyRate,
                activeCount
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_ANALYTICS_TELEMETRY, unitName, metric);

        if (future != null) {
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[Kafka Producer] Métrica emitida a '{}' [Unidad: {}, Ocupación: {}%]",
                            KafkaProducerConfig.TOPIC_ANALYTICS_TELEMETRY, unitName, occupancyRate);
                } else {
                    log.error("[Kafka Producer] Fallo al emitir métrica a Kafka: {}", ex.getMessage());
                }
            });
        }
    }
}
