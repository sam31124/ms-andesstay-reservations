package com.andesstay.reservations.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnvelope<T> implements Serializable {

    private String eventId;
    private String type;
    private String timestamp;
    private String traceId;
    private String correlationId;
    private T payload;

    public static <T> MessageEnvelope<T> of(String type, T payload) {
        String corrId = UUID.randomUUID().toString();
        return MessageEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .timestamp(Instant.now().toString())
                .traceId("trace-" + corrId.substring(0, 8))
                .correlationId(corrId)
                .payload(payload)
                .build();
    }

    public static <T> MessageEnvelope<T> of(String type, String correlationId, T payload) {
        return MessageEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .timestamp(Instant.now().toString())
                .traceId("trace-" + correlationId.substring(0, 8))
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
