package com.andesstay.reservations.controller;

import com.andesstay.reservations.entity.Reservation;
import com.andesstay.reservations.model.MessageEnvelope;
import com.andesstay.reservations.repository.ReservationRepository;
import com.andesstay.reservations.service.ReservationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher reservationEventPublisher;
    private final com.andesstay.reservations.service.KafkaReservationStreamProducer kafkaStreamProducer;

    public ReservationController(ReservationRepository reservationRepository,
                                 ReservationEventPublisher reservationEventPublisher,
                                 com.andesstay.reservations.service.KafkaReservationStreamProducer kafkaStreamProducer) {
        this.reservationRepository = reservationRepository;
        this.reservationEventPublisher = reservationEventPublisher;
        this.kafkaStreamProducer = kafkaStreamProducer;
    }

    @GetMapping
    public ResponseEntity<?> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        return ResponseEntity.ok(Map.of(
            "message", "Obtención exitosa de reservas desde Base de Datos",
            "data", reservations
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .map(r -> ResponseEntity.ok(Map.of("data", r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Map<String, Object> body) {
        String huesped = body.containsKey("huesped") ? String.valueOf(body.get("huesped")) : (body.containsKey("guestName") ? String.valueOf(body.get("guestName")) : null);
        String unidad = body.containsKey("unidad") ? String.valueOf(body.get("unidad")) : (body.containsKey("room") ? String.valueOf(body.get("room")) : null);
        String estado = body.containsKey("estado") ? String.valueOf(body.get("estado")) : (body.containsKey("status") ? String.valueOf(body.get("status")) : null);
        boolean simulateError = Boolean.parseBoolean(String.valueOf(body.getOrDefault("simulateError", "false")));

        if (huesped == null || huesped.isBlank() || unidad == null || unidad.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Los campos de huésped y unidad/habitación son obligatorios."
            ));
        }

        Reservation newReservation = new Reservation();
        newReservation.setHuesped(huesped.trim());
        newReservation.setUnidad(unidad.trim());
        newReservation.setEstado((estado != null && !estado.isBlank()) ? estado.toUpperCase().trim() : "CREADA");

        Reservation savedReservation = reservationRepository.save(newReservation);

        // Desacoplamiento asíncrono con RabbitMQ (AndesStay EP3 - Amaro Ulloa)
        try {
            reservationEventPublisher.publishReservationCreated(savedReservation, simulateError);
        } catch (Exception e) {
            log.warn("[RabbitMQ Warning] No se pudo publicar evento asíncrono: {}", e.getMessage());
        }

        // Streaming en tiempo real con Apache Kafka (AndesStay EP5)
        try {
            kafkaStreamProducer.publishReservationEvent(savedReservation, "RESERVATION_CREATED");
            kafkaStreamProducer.publishTelemetryMetric(savedReservation.getUnidad(), 82.5, 1);
        } catch (Exception e) {
            log.warn("[Kafka Warning] No se pudo emitir evento de streaming a Kafka: {}", e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Reserva creada con éxito, tareas asíncronas despachadas a RabbitMQ y streaming emitido a Kafka",
            "data", savedReservation,
            "asyncDispatched", true,
            "queuesNotified", List.of("q.cmd.email", "q.cmd.housekeeping", "q.cmd.voucher"),
            "kafkaTopicsNotified", List.of("andesstay.reservations.stream", "andesstay.analytics.telemetry")
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String newStatus = body.containsKey("status") ? String.valueOf(body.get("status")) : null;
        boolean simulateError = Boolean.parseBoolean(String.valueOf(body.getOrDefault("simulateError", "false")));

        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'status' es obligatorio."));
        }

        return reservationRepository.findById(id).map(reserva -> {
            // Regla de negocio AndesStay: No se puede hacer check-in sin CONFIRMAR
            if ("CHECKIN_PENDIENTE".equalsIgnoreCase(newStatus) && !"CONFIRMADA".equalsIgnoreCase(reserva.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Regla de negocio violada: No se puede pasar a CHECKIN_PENDIENTE si la reserva no está CONFIRMADA."
                ));
            }

            String oldStatus = reserva.getEstado();
            reserva.setEstado(newStatus.toUpperCase().trim());
            reservationRepository.save(reserva);

            // Publicación asíncrona de cambio de estado (RabbitMQ)
            try {
                reservationEventPublisher.publishReservationStatusUpdated(reserva, oldStatus, simulateError);
            } catch (Exception e) {
                log.warn("[RabbitMQ Warning] No se pudo publicar cambio de estado: {}", e.getMessage());
            }

            // Streaming de actualización a Kafka (AndesStay EP5)
            try {
                kafkaStreamProducer.publishReservationEvent(reserva, "RESERVATION_STATUS_UPDATED");
            } catch (Exception e) {
                log.warn("[Kafka Warning] No se pudo emitir evento a Kafka: {}", e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "message", "Estado de reserva actualizado con éxito y notificado asíncronamente",
                "data", reserva,
                "oldStatus", oldStatus,
                "newStatus", reserva.getEstado(),
                "asyncDispatched", true,
                "kafkaStreamEvent", "RESERVATION_STATUS_UPDATED"
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/kafka/simulate-stream")
    public ResponseEntity<?> simulateKafkaStream(
            @RequestParam(defaultValue = "RESERVATION_STREAM_TEST") String eventType,
            @RequestParam(defaultValue = "Cabaña Vista Volcán") String unitName) {
        try {
            Reservation dummy = new Reservation();
            dummy.setId(999L);
            dummy.setHuesped("Stream Test Guest");
            dummy.setUnidad(unitName);
            dummy.setEstado("CONFIRMADA");

            kafkaStreamProducer.publishReservationEvent(dummy, eventType);
            kafkaStreamProducer.publishTelemetryMetric(unitName, 91.0, 5);

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Evento de streaming y métricas emitidos a Kafka con éxito",
                "topics", List.of("andesstay.reservations.stream", "andesstay.analytics.telemetry"),
                "eventType", eventType,
                "unitName", unitName
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Fallo al emitir evento a Kafka: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/simulate-dlq")
    public ResponseEntity<?> simulateDlq(@RequestParam(defaultValue = "email") String queue) {
        try {
            MessageEnvelope<Map<String, Object>> envelope = reservationEventPublisher.publishSimulatedDlqMessage(queue);
            return ResponseEntity.ok(Map.of(
                "message", "Mensaje de prueba emitido con error inducido para DLQ",
                "targetQueueType", queue,
                "eventId", envelope.getEventId(),
                "correlationId", envelope.getCorrelationId(),
                "payload", envelope.getPayload()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error al enviar mensaje a RabbitMQ: " + e.getMessage()
            ));
        }
    }
}
