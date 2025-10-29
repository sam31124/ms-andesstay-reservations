package com.andesstay.reservations.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class ReservationStreamEvent implements Serializable {

    private String eventId;
    private String eventType; // RESERVATION_CREATED, RESERVATION_UPDATED, CHECKOUT_COMPLETED
    private String timestamp;
    private Long reservationId;
    private String guestName;
    private String unitName;
    private String status;
    private Double totalAmount;
    private Integer nights;
    private String traceId;

    public ReservationStreamEvent() {
    }

    public ReservationStreamEvent(String eventId, String eventType, String timestamp, Long reservationId,
                                  String guestName, String unitName, String status, Double totalAmount,
                                  Integer nights, String traceId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.unitName = unitName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.nights = nights;
        this.traceId = traceId;
    }

    public static ReservationStreamEvent of(String eventType, Long reservationId, String guestName,
                                            String unitName, String status, Double totalAmount, Integer nights) {
        return new ReservationStreamEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                reservationId,
                guestName,
                unitName,
                status,
                totalAmount != null ? totalAmount : 85000.0,
                nights != null ? nights : 2,
                "TRACE-" + UUID.randomUUID().toString().substring(0, 8)
        );
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
