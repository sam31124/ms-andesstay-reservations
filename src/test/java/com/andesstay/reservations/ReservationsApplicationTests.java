package com.andesstay.reservations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void testGetAllReservations() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testCreateReservation() throws Exception {
        String json = "{\"huesped\":\"Pedro Pascal\",\"unidad\":\"Cabaña Vista Volcán\",\"estado\":\"CREADA\"}";
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.huesped").value("Pedro Pascal"));
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
}
