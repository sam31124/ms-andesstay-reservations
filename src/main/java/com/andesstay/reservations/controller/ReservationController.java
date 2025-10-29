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

    public ReservationController(ReservationRepository reservationRepository,
                                 ReservationEventPublisher reservationEventPublisher) {
        this.reservationRepository = reservationRepository;
        this.reservationEventPublisher = reservationEventPublisher;
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

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Reserva creada con éxito y tareas asíncronas despachadas a RabbitMQ",
            "data", savedReservation,
            "asyncDispatched", true,
            "queuesNotified", List.of("q.cmd.email", "q.cmd.housekeeping", "q.cmd.voucher")
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

            // Publicación asíncrona de cambio de estado
            try {
                reservationEventPublisher.publishReservationStatusUpdated(reserva, oldStatus, simulateError);
            } catch (Exception e) {
                log.warn("[RabbitMQ Warning] No se pudo publicar cambio de estado: {}", e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "message", "Estado de reserva actualizado con éxito y notificado asíncronamente",
                "data", reserva,
                "oldStatus", oldStatus,
                "newStatus", reserva.getEstado(),
                "asyncDispatched", true
            ));
        }).orElse(ResponseEntity.notFound().build());
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
