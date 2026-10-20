# ms-pedidos360-mq-admin

Microservicio **administrador de RabbitMQ** de Pedidos360. Expone una API REST para crear, consultar y eliminar colas, exchanges y bindings, y para publicar mensajes de prueba. Es de uso interno: el frontend llega a él a través del BFF (`/api/mq/**`), que solo lo permite al rol **Admin** después de validar el JWT de Entra ID.

## API

Swagger UI: `http://localhost:8086/swagger-ui.html`

| Método | Ruta | Respuesta | Errores |
|---|---|---|---|
| GET | `/api/mq/queues` | 200 · colas con mensajes y consumidores | 503 |
| GET | `/api/mq/queues/{name}` | 200 | 404 |
| POST | `/api/mq/queues` | 201 + `Location` | 400, 409 (ya existe), 422 (el DLX no existe) |
| DELETE | `/api/mq/queues/{name}` | 204 | 404, 409 (cola del sistema) |
| DELETE | `/api/mq/queues/{name}/messages` | 200 · vacía la cola (p. ej. una DLQ revisada) | 404 |
| GET | `/api/mq/exchanges` | 200 (sin los internos `amq.*`) | 503 |
| POST | `/api/mq/exchanges` | 201 + `Location` | 400, 409 |
| DELETE | `/api/mq/exchanges/{name}` | 204 | 404, 409 (exchange del sistema) |
| GET | `/api/mq/bindings` | 200 | 503 |
| POST | `/api/mq/bindings` | 201 | 400, 404 (exchange o cola inexistente), 409 |
| DELETE | `/api/mq/bindings?exchange=&queue=&routingKey=` | 204 | 400, 404 |
| POST | `/api/mq/messages` | 202 · publica un mensaje de prueba con el envelope del sistema | 400, 404 |

Ejemplos:

```http
POST /api/mq/queues
{ "name": "q.demo", "durable": true, "deadLetterExchange": "cmd.dead.dlx", "deadLetterRoutingKey": "demo", "messageTtlMs": 60000 }

POST /api/mq/exchanges
{ "name": "ex.demo", "type": "topic" }

POST /api/mq/bindings
{ "exchange": "ex.demo", "queue": "q.demo", "routingKey": "demo.#" }

POST /api/mq/messages
{ "exchange": "cmd.direct", "routingKey": "email.send", "payload": { "orderId": 1, "status": "ACEPTADO", "simulateFailure": "transient" } }
```

## Validación de entrada

Los DTO (`dto/`) usan Bean Validation, y cada error responde con un mensaje que explica la causa:

| Campo | Regla |
|---|---|
| `name`, `exchange`, `queue`, `deadLetterExchange` | Obligatorio (salvo el DLX), máximo 200 caracteres, solo `A-Z a-z 0-9 . _ -`, no puede empezar con `amq.` (reservado por RabbitMQ) |
| `routingKey`, `deadLetterRoutingKey` | Máximo 255 caracteres, `A-Z a-z 0-9 . _ - * #`. Puede ser vacía (fanout) |
| `type` (exchange) | `direct`, `topic`, `fanout` o `headers` |
| `messageTtlMs` | Entre 1 y 86.400.000 ms (24 h) |
| `maxLength` | Entre 1 y 1.000.000 |
| `deadLetterRoutingKey` | Solo se acepta si también viene `deadLetterExchange` |
| `payload` (mensaje) | Obligatorio, objeto no vacío |

Además, el servicio aplica reglas que dependen del estado del broker. No se crean recursos que ya existen (409), el dead-letter exchange debe existir (422) y los bindings solo unen exchanges y colas existentes (404). Las colas y exchanges de la topología del sistema (`pedidos360.admin.protected-*`) no se pueden eliminar (409).

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "La petición tiene datos inválidos",
  "path": "/api/mq/queues", "details": ["name: es obligatorio"] }
```

## Diseño

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controladores | `QueueController`, `ExchangeController`, `BindingController`, `MessageController` | Rutas REST, `@Valid`, códigos HTTP. No conocen RabbitMQ |
| Servicio | `RabbitAdminService` | **Toda** la lógica de administración: usa `AmqpAdmin` para declarar y eliminar, y valida existencia y recursos protegidos |
| Cliente | `RabbitManagementClient` | Lecturas a través de la API HTTP del plugin de administración (AMQP no permite listar) |
| Errores | `GlobalExceptionHandler` | 400, 404, 409, 422, 502 y 503 con un cuerpo uniforme |
| Config | `AdminProperties` | `pedidos360.admin.*` en `application.yml` |

## Variables de entorno

| Variable | Por defecto |
|---|---|
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_MANAGEMENT_PORT` | `15672` |
| `RABBITMQ_USER` | `pedidos360` |
| `RABBITMQ_PASSWORD` | `pedidos360` |

## Ejecutar

```bash
./mvnw test                 # 28 pruebas: validación, códigos HTTP y reglas del servicio
./mvnw spring-boot:run      # puerto 8086; requiere RabbitMQ con el plugin de administración
```

## CI/CD

`.github/workflows/ci-cd.yml` corre las pruebas en cada push. En `main`, además, despliega en la EC2 con `docker compose up -d --build mq-admin`.

## Autores

Germán Marabolí, Sebastián Velez & Camila Vera

Proyecto Pedidos360 · DSY1107 Desarrollo Cloud Native I · Duoc UC
