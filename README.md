# 🔔 Pasa La Peli - Notification Service

Microservicio de **notificaciones in-app por usuario** del ecosistema Cloud Native **Pasa La Peli**.

Consume **eventos asincrónicos** de negocio desde **AWS SQS** (películas agregadas, tickets pagados y tickets devueltos), persiste notificaciones **independientes por usuario** en MySQL y expone una API consumida por el BFF para la campana/bandeja del frontend.

---

## 🚀 Tecnologías
- **Java 21**
- **Spring Boot 3.3.3**
- **Spring Cloud AWS SQS**
- **Spring Data JPA / Hibernate**
- **MySQL 8.0**
- **Bean Validation**
- **Docker & Dockerfile**

---

## 🧭 Rol en la arquitectura

```
movie-service ─┐                        ┌─> Notificacion (BBDD, por usuario)
               ├─(AWS SNS→SQS)──> notification-service
ticket-service ┘   pasalapeli-events    └─> DLQ (MensajeFallido)
        ▲                                        ▲
        │ eventos publicados tras commit         │ API interna
   BFF /<api/notificaciones> ────────────────────┘
```

- Los **productores** (`movie-service`, `ticket-service`) publican eventos genéricos en el topic SNS `pasalapeli-events` **después de confirmar la transacción** (`@TransactionalEventListener(AFTER_COMMIT)`); una suscripción con FilterPolicy los enruta a la cola SQS.
- El **consumidor** (`NotificacionConsumer`) resuelve los destinatarios según las **reglas de distribución** administrables y genera **una notificación por usuario**.
- Si el procesamiento falla, SQS reintenta y deriva el mensaje a la **Dead-Letter Queue** (redrive `maxReceiveCount=3`); el poller `DeadLetterConsumer` los registra en `MensajeFallido`.

---

## 📚 Topología de mensajería (creada por `setup-messaging.sh` del orquestador)

| Elemento | Nombre | Tipo |
|---|---|---|
| Topic | `pasalapeli-events` | SNS topic |
| Cola | `pasalapeli-notificaciones` | SQS (VisibilityTimeout 60 s, retención 4 d) |
| Dead-Letter Queue | `pasalapeli-notificaciones-dlq` | SQS (retención 14 d, redrive maxReceiveCount=3) |
| Suscripción | SNS→SQS | FilterPolicy `{"tipo":["PELICULA_CREADA","TICKET_PAGADO","TICKET_DEVUELTO"]}`, `RawMessageDelivery=true` |

### Tipos de evento
| Evento | Atributo SQS `tipo` | Destino por defecto (seed) | Notifica a |
|---|---|---|---|
| `PELICULA_CREADA` | `PELICULA_CREADA` | `Todos los usuarios` (TODOS) | Todos los usuarios |
| `TICKET_PAGADO` | `TICKET_PAGADO` | `Usuario del evento` (USUARIO_EVENTO) | Comprador |
| `TICKET_DEVUELTO` | `TICKET_DEVUELTO` | `Usuario del evento` (USUARIO_EVENTO) | Comprador |

---

## ⚙️ Configuración (`application.yml`)
- **Puerto:** `8084`
- **Base de Datos:** `jdbc:mysql://localhost:3306/pasalapeli_db` (Usuario: `root`, Password: `root`)
- **AWS SQS:** región `AWS_REGION` (default `us-east-1`); credenciales por la cadena por defecto (env / profile / IMDS).

Variables de entorno relevantes: `SPRING_DATASOURCE_*`, `AWS_REGION`, `SQS_QUEUE_URL`, `SQS_DLQ_URL`, `SQS_DLQ_NAME` (generadas por `scripts/setup-messaging.sh` del orquestador).

---

## 📡 Endpoints

### API interna (consumida por el BFF)
- `GET /api/notificaciones/usuario/{usuarioId}`: Lista las notificaciones del usuario (más recientes primero).
- `GET /api/notificaciones/usuario/{usuarioId}/no-leidas/count`: Contador de no leídas (`{ "count": N }`).
- `POST /api/notificaciones/{id}/leer?usuarioId={id}`: Marca una notificación como leída.
- `POST /api/notificaciones/usuario/{usuarioId}/leer-todas`: Marca todas como leídas.

### API de administración
- `GET /api/notificaciones/admin/destinos` · `POST` · `DELETE /{id}`: CRUD de destinos (`TODOS`, `ROL`, `USUARIO`, `USUARIO_EVENTO`).
- `GET /api/notificaciones/admin/reglas` · `POST` · `DELETE /{id}`: Reglas evento → destino.
- `GET /api/notificaciones/admin/mensajes-fallidos` · `DELETE /{id}`: Diagnóstico de la Dead-Letter Queue.

> El BFF expone estas operaciones al frontend en `/api/notificaciones` (usuario) y `/api/admin/{destinos,reglas,mensajes-fallidos}` (solo `ADMIN`).

---

## 🗄️ Modelo de datos (BBDD `pasalapeli_db`)
- `Notificacion`: notificación individual por usuario (`usuario_id`, `tipo`, `titulo`, `mensaje`, `referencia`, `leida`, `fecha_creacion`).
- `DestinoNotificacion`: destinos administrables de notificación.
- `ReglaDistribucion`: asociación evento → destino (enrutamiento).
- `MensajeFallido`: registro de mensajes derivados a la DLQ.
- `Usuario`: tabla compartida (solo lectura) para resolver destinatarios por rol.

Esquema definido en [`pasalapeli-database/init.sql`](https://github.com) (incluye destinos y reglas semilla).

---

## 🛠️ Ejecución Local
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```
O con Docker:
```bash
docker build -t notification-service .
docker run -p 8084:8084 notification-service
```

Requiere acceso a la cola SQS (y su DLQ) definida por `setup-messaging.sh` y la BBDD `pasalapeli_db` con el esquema cargado. En local sin AWS, el contexto levanta pero el listener de SQS puede desactivarse con `spring.cloud.aws.sqs.listener.auto-startup=false` (como en los tests).

---

## 🧪 Mensajes de prueba (verificación de la topología)

Publicar un mensaje manual en SNS permite probar el flujo completo **sin tocar el negocio** (la suscripción con FilterPolicy lo entrega en la cola y el listener lo procesa):

```bash
aws sns publish \
  --topic-arn "$SNS_TOPIC_ARN" \
  --message '{"tipo":"PELICULA_CREADA","usuarioId":null,"codigoTicket":null,"detalle":"Película de prueba"}' \
  --message-attributes tipo={DataType=String,StringValue=PELICULA_CREADA}
```

Qué debe ocurrir (en este orden):
1. El mensaje aparece en `pasalapeli-notificaciones` y **desaparece** al procesarse.
2. Se inserta una fila en `Notificacion` por cada destinatario resuelto por la regla (`TODOS` → todos los usuarios activos).
3. El contador de la campana del frontend baja al abrirla/marcar como leída (en ≤ 10 s).

**Prueba de la DLQ (reintentos/fallidos):** publicar un payload que no sea JSON válido (por ejemplo `esto no es un json valido {{`) hace fallar la deserialización → SQS reintenta (`maxReceiveCount=3`) y deriva el mensaje a `pasalapeli-notificaciones-dlq`; el poller lo registra en `GET /api/notificaciones/admin/mensajes-fallidos` (tabla `MensajeFallido`) con cola, payload crudo, error e intentos para diagnosticarlo. (Un evento con tipo desconocido, en cambio, **no** falla: cae en la distribución por defecto.)

---

## ☁️ Despliegue CI/CD (GitHub Actions → EC2)

Este repo se despliega **solo a sí mismo** sobre la instancia **EC2-APPS (Ubuntu 24.04)** que ya porta el stack completo. El orquestador vive en el repo [`pasalapeli-database`](https://github.com) (`docker-compose.yml` global: servicios + mensajería AWS SNS/SQS).

### Workflow `.github/workflows/deploy.yml`
En cada `push` a `main`:
1. SSH al EC2 (acción `appleboy/ssh-action`).
2. `git pull` del código en `/opt/pasalapeli/pasalapeli-notification-service` (**clona el repo con `git clone` si el directorio no existe todavía**, porque el bootstrap no lo trae en la primera pasada).
3. `docker compose up -d --build notification-service`.
4. Espera el estado `healthy` del contenedor vía `/actuator/health`.

### GitHub Secrets requeridos en este repo
| Secret | Descripción |
|---|---|
| `EC2_HOST` | IP pública del EC2 |
| `EC2_USER` | Usuario SSH (usualmente `ubuntu`) |
| `EC2_SSH_KEY` | Clave privada SSH (.pem) |

### Variables de entorno en producción (definidas en el `.env` del orquestador)
- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `AWS_REGION=us-east-1`
- `SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/<account>/pasalapeli-notificaciones`
- `SQS_DLQ_URL=https://sqs.us-east-1.amazonaws.com/<account>/pasalapeli-notificaciones-dlq`
