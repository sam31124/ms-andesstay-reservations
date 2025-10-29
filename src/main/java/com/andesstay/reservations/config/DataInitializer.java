package com.andesstay.reservations.config;

import com.andesstay.reservations.entity.Reservation;
import com.andesstay.reservations.repository.ReservationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(ReservationRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Reservation("Juan Perez", "Cabaña Vista Volcán", "CONFIRMADA"));
                repository.save(new Reservation("Maria Lopez", "Habitación 102 Standard", "CREADA"));
                repository.save(new Reservation("Carlos Urzua", "Lodge Bosque Nativo", "EN_ESTADIA"));
            }
        };
    }
}
