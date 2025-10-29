package com.andesstay.reservations.service;

import com.andesstay.reservations.config.RabbitConfig;
import com.andesstay.reservations.entity.Reservation;
import com.andesstay.reservations.model.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ReservationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica eventos asíncronos cuando se crea una reserva:
     * 1. q.cmd.email: Envío de correo de confirmación al huésped.
     * 2. q.cmd.housekeeping: Solicitud de preparación de unidad/habitación.
     * 3. q.cmd.voucher: Generación asíncrona de comprobante/voucher en PDF.
     */
    public void publishReservationCreated(Reservation reservation, boolean simulateError) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reservationId", reservation.getId());
        payload.put("huesped", reservation.getHuesped());
        payload.put("unidad", reservation.getUnidad());
        payload.put("estado", reservation.getEstado());
        payload.put("email", reservation.getHuesped().toLowerCase().replace(" ", ".") + "@example.com");
        payload.put("simulateError", simulateError);

        // 1. Notificación Email
        MessageEnvelope<Map<String, Object>> emailMsg = MessageEnvelope.of("EMAIL_CONFIRMACION_RESERVA", payload);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_EMAIL_SEND, emailMsg);
        log.info("[RabbitMQ Producer] Mensaje emitido a '{}' con RK '{}' [EventId: {}]",
                RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_EMAIL_SEND, emailMsg.getEventId());

        // 2. Ticket Housekeeping
        MessageEnvelope<Map<String, Object>> hkMsg = MessageEnvelope.of("HOUSEKEEPING_PREPARACION", payload);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_HOUSEKEEPING_TICKET, hkMsg);
        log.info("[RabbitMQ Producer] Mensaje emitido a '{}' con RK '{}' [EventId: {}]",
                RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_HOUSEKEEPING_TICKET, hkMsg.getEventId());

        // 3. Generación Voucher PDF
        MessageEnvelope<Map<String, Object>> voucherMsg = MessageEnvelope.of("VOUCHER_GENERACION", payload);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_VOUCHER_GEN, voucherMsg);
        log.info("[RabbitMQ Producer] Mensaje emitido a '{}' con RK '{}' [EventId: {}]",
                RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_VOUCHER_GEN, voucherMsg.getEventId());
    }

    /**
     * Publica eventos asíncronos según el cambio de estado de la reserva.
     */
    public void publishReservationStatusUpdated(Reservation reservation, String oldStatus, boolean simulateError) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reservationId", reservation.getId());
        payload.put("huesped", reservation.getHuesped());
        payload.put("unidad", reservation.getUnidad());
        payload.put("estadoAnterior", oldStatus);
        payload.put("nuevoEstado", reservation.getEstado());
        payload.put("email", reservation.getHuesped().toLowerCase().replace(" ", ".") + "@example.com");
        payload.put("simulateError", simulateError);

        String status = reservation.getEstado();

        // Si es CHECKOUT -> Limpieza de habitación y voucher final
        if ("CHECKOUT".equalsIgnoreCase(status)) {
            MessageEnvelope<Map<String, Object>> hkMsg = MessageEnvelope.of("HOUSEKEEPING_LIMPIEZA_CHECKOUT", payload);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_HOUSEKEEPING_TICKET, hkMsg);
            log.info("[RabbitMQ Producer] Solicitud de limpieza post checkout enviada [EventId: {}]", hkMsg.getEventId());
        }

        // En todo cambio de estado se notifica al huésped por email
        MessageEnvelope<Map<String, Object>> emailMsg = MessageEnvelope.of("EMAIL_ESTADO_ACTUALIZADO", payload);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_DIRECT, RabbitConfig.ROUTING_KEY_EMAIL_SEND, emailMsg);
        log.info("[RabbitMQ Producer] Notificación de cambio de estado enviada [EventId: {}]", emailMsg.getEventId());
    }

    /**
     * Dispara un mensaje con error intencional para demostrar el flujo hacia la Dead Letter Queue (DLQ).
     */
    public MessageEnvelope<Map<String, Object>> publishSimulatedDlqMessage(String queueType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("simulateError", true);
        payload.put("reason", "Simulación de fallo para comprobación de Dead Letter Queue (DLQ)");
        payload.put("huesped", "Error Test Guest");
        payload.put("unidad", "Habitacion Fallida 999");
        payload.put("email", "error.dlq@fallido.cl");

        String routingKey = switch (queueType.toLowerCase()) {
            case "housekeeping" -> RabbitConfig.ROUTING_KEY_HOUSEKEEPING_TICKET;
            case "voucher" -> RabbitConfig.ROUTING_KEY_VOUCHER_GEN;
            default -> RabbitConfig.ROUTING_KEY_EMAIL_SEND;
        };

        MessageEnvelope<Map<String, Object>> msg = MessageEnvelope.of("SIMULATED_ERROR_TEST", payload);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_DIRECT, routingKey, msg);

        log.warn("[RabbitMQ Producer] Mensaje de PRUEBA DLQ emitido a '{}' con RK '{}' [EventId: {}]",
                RabbitConfig.EXCHANGE_DIRECT, routingKey, msg.getEventId());

        return msg;
    }
}
