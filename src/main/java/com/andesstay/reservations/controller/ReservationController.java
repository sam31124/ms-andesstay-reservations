package com.andesstay.reservations.controller;

import com.andesstay.reservations.entity.Reservation;
import com.andesstay.reservations.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;

    public ReservationController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
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
    public ResponseEntity<?> createReservation(@RequestBody Map<String, String> body) {
        String huesped = body.getOrDefault("huesped", body.get("guestName"));
        String unidad = body.getOrDefault("unidad", body.get("room"));
        String estado = body.getOrDefault("estado", body.get("status"));

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

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Reserva creada con éxito",
            "data", savedReservation
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
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

            reserva.setEstado(newStatus.toUpperCase());
            reservationRepository.save(reserva);
            return ResponseEntity.ok(Map.of(
                "message", "Estado de reserva actualizado con éxito",
                "data", reserva
            ));
        }).orElse(ResponseEntity.notFound().build());
    }
}
