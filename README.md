# Microservicio de Reservas — AndesStay (`ms-andesstay-reservations`)

**Proyecto:** AndesStay (Caso 5 - Desarrollo Cloud Native I - DSY1107)  
**Evaluación:** Evaluación Parcial N°3 (EP3 - 12%)  
**Responsable:** Amaro Ulloa  
**Puerto:** `8081`  
**Tecnología:** Java 17/21, Spring Boot 3.3.4, Spring Data JPA, Spring AMQP (RabbitMQ)

---

## 1. Descripción
Este microservicio gestiona el ciclo de vida del dominio central de Reservas y el **desacoplamiento asíncrono** mediante publicación de mensajes a RabbitMQ.

Cuando se crea una reserva (`POST /api/reservations`) o se actualiza su estado (`PUT /api/reservations/{id}/status`), el microservicio persiste en base de datos H2/JPA y de forma no bloqueante publica un evento estandarizado con el patrón **`MessageEnvelope`** hacia:
1. `q.cmd.email` (RK: `email.send`): Notificación de confirmación o cambio de estado al huésped.
2. `q.cmd.housekeeping` (RK: `housekeeping.ticket`): Generación de orden de preparación de habitación o limpieza post-checkout.
3. `q.cmd.voucher` (RK: `voucher.gen`): Generación de voucher/comprobante en PDF.

---

## 2. Topología Declarada en `RabbitConfig.java`
- **Exchanges:**
  - `cmd.direct` (tipo direct)
  - `cmd.topic` (tipo topic)
  - `cmd.dead.dlx` (Dead Letter Exchange)
- **Colas Principales con Dead Letter Exchange (DLX) configurado:**
  - `q.cmd.email` -> DLQ: `q.cmd.email.dlq`
  - `q.cmd.housekeeping` -> DLQ: `q.cmd.housekeeping.dlq`
  - `q.cmd.voucher` -> DLQ: `q.cmd.voucher.dlq`

---

## 3. Formato del Envelope de Mensajes (`MessageEnvelope`)
```json
{
  "eventId": "a7b3c9f2-...",
  "type": "EMAIL_CONFIRMACION_RESERVA",
  "timestamp": "2026-10-14T22:00:00Z",
  "traceId": "trace-a7b3c9f2",
  "correlationId": "a7b3c9f2-...",
  "payload": {
    "reservationId": 1,
    "huesped": "Juan Pérez",
    "unidad": "Cabaña Vista Volcán",
    "estado": "CREADA",
    "email": "juan.perez@example.com",
    "simulateError": false
  }
}
```

---

## 4. Comandos de Compilación y Pruebas
Para compilar y ejecutar las pruebas unitarias:
```powershell
.\mvnw.cmd clean test
```
Para ejecutar el microservicio en local:
```powershell
.\mvnw.cmd spring-boot:run
```
