package com.andesstay.reservations;

import com.andesstay.reservations.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ConnectionFactory connectionFactory;

    @Test
    void testGetAllReservations() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testCreateReservation_PublishesToRabbitMQ() throws Exception {
        String json = "{\"huesped\":\"Pedro Pascal\",\"unidad\":\"Cabaña Vista Volcán\",\"estado\":\"CREADA\"}";
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.huesped").value("Pedro Pascal"))
                .andExpect(jsonPath("$.asyncDispatched").value(true));

        // Verifica que el productor despachó los mensajes a las colas correspondientes
        Mockito.verify(rabbitTemplate, Mockito.atLeastOnce())
                .convertAndSend(eq(RabbitConfig.EXCHANGE_DIRECT), eq(RabbitConfig.ROUTING_KEY_EMAIL_SEND), any(Object.class));
        Mockito.verify(rabbitTemplate, Mockito.atLeastOnce())
                .convertAndSend(eq(RabbitConfig.EXCHANGE_DIRECT), eq(RabbitConfig.ROUTING_KEY_HOUSEKEEPING_TICKET), any(Object.class));
        Mockito.verify(rabbitTemplate, Mockito.atLeastOnce())
                .convertAndSend(eq(RabbitConfig.EXCHANGE_DIRECT), eq(RabbitConfig.ROUTING_KEY_VOUCHER_GEN), any(Object.class));
    }

    @Test
    void testCheckInWithoutConfirm_Fails() throws Exception {
        // Reserva 2 inicia en CREADA, pasar a CHECKIN_PENDIENTE debe fallar con 400
        String json = "{\"status\":\"CHECKIN_PENDIENTE\"}";
        mockMvc.perform(put("/api/reservations/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testSimulateDlqEndpoint() throws Exception {
        mockMvc.perform(post("/api/reservations/simulate-dlq?queue=email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.targetQueueType").value("email"));

        Mockito.verify(rabbitTemplate, Mockito.atLeastOnce())
                .convertAndSend(eq(RabbitConfig.EXCHANGE_DIRECT), eq(RabbitConfig.ROUTING_KEY_EMAIL_SEND), any(Object.class));
    }
}
