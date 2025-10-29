package com.andesstay.reservations.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class AnalyticsTelemetryEvent implements Serializable {

    private String eventId;
    private String metricType; // OCCUPANCY_RATE, REALTIME_BOOKING_VELOCITY
    private String timestamp;
    private String unitName;
    private Double occupancyRate;
    private Integer activeReservations;
    private String zone;

    public AnalyticsTelemetryEvent() {
    }

    public AnalyticsTelemetryEvent(String eventId, String metricType, String timestamp,
                                   String unitName, Double occupancyRate, Integer activeReservations, String zone) {
        this.eventId = eventId;
        this.metricType = metricType;
        this.timestamp = timestamp;
        this.unitName = unitName;
        this.occupancyRate = occupancyRate;
        this.activeReservations = activeReservations;
        this.zone = zone;
    }

    public static AnalyticsTelemetryEvent of(String metricType, String unitName, Double occupancyRate, Integer activeReservations) {
        return new AnalyticsTelemetryEvent(
                UUID.randomUUID().toString(),
                metricType,
                Instant.now().toString(),
                unitName,
                occupancyRate,
                activeReservations,
                "ZONA_CENTRO_SUR"
        );
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public Double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(Double occupancyRate) { this.occupancyRate = occupancyRate; }

    public Integer getActiveReservations() { return activeReservations; }
    public void setActiveReservations(Integer activeReservations) { this.activeReservations = activeReservations; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
}
