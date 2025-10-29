# ms-andesstay-reservations - Microservicio de Gestión de Reservas

**Asignatura:** DSY1107 - Desarrollo Cloud Native I  
**Evaluación:** Evaluación Parcial N°1 (EP1)  
**Caso de Estudio:** Caso 5 - Red de Hoteles, Cabañas y Lodges AndesStay  
**Alumno:** Samuel Urzua Moraga (`sa.urzua@duocuc.cl`)  

---

## 📋 Descripción

Microservicio de dominio desarrollado en **Spring Boot 3 (Java 21)** responsable del ciclo de vida y persistencia de las reservas de hospedaje para la red AndesStay.

---

## ⚙️ Especificaciones Técnicas

- **Puerto:** `8081`
- **Persistencia:** Spring Data JPA + Hibernate
- **Base de Datos:** H2 Database en memoria (compatible con migración a base de datos Cloud/Oracle)
- **Consola H2:** `/h2-console` (JDBC URL: `jdbc:h2:mem:reservationsdb`)

---

## 🔄 Máquina de Estados de la Reserva

El microservicio controla la transición de estados según las reglas de negocio de AndesStay:
`CREADA` ➔ `CONFIRMADA` ➔ `CHECKIN_PENDIENTE` ➔ `EN_ESTADIA` ➔ `CHECKOUT` / `CANCELADA`

---

## 🔌 Endpoints REST

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/reservations` | Listar todas las reservas |
| `GET` | `/api/reservations/{id}` | Obtener detalle de una reserva |
| `POST` | `/api/reservations` | Crear nueva reserva |
| `PUT` | `/api/reservations/{id}/status` | Cambiar el estado de la reserva |

---

## 🧪 Pruebas Automatizadas

El proyecto cuenta con pruebas de integración con `@SpringBootTest` y `MockMvc`:

```bash
.\mvnw.cmd test
```

### Pruebas incluidas:
- `testFindAllReservations()`: Validación de consulta y serialización de reservas.
- `testUpdateReservationStatus()`: Validación de actualización de estado y persistencia.
- `testCreateReservation()`: Validación de inserción de nuevas reservas en base de datos.
