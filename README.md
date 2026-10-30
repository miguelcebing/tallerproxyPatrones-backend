# Taller Patrón Proxy — Backend

Sistema de monitoreo de microservicios con **Proxy de auditoría y logging**. Un `LoggingProxy<T>` se
coloca delante de cada microservicio (Inventario, Pedidos, Pagos) e intercepta todas las llamadas para
registrar métricas de uso, tiempos de respuesta y errores. El frontend consume estos datos por HTTP/JSON.

> Este taller trata de **logging**, no de login: no existen usuarios, contraseñas ni autenticación.

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 3.5.3 (Spring Web + Actuator) |
| Maven | 3.10 (wrapper incluido: `mvnw`) |
| JUnit 5 / AssertJ / Mockito / MockMvc | gestionados por Spring Boot |

## Cómo ejecutar en local

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080` y la salud en `http://localhost:8080/actuator/health`.

Pruebas:

```bash
./mvnw test          # 37 pruebas
./mvnw clean package # genera target/tallerproxy-backend-1.0.0.jar
```

## Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `PORT` | Puerto HTTP (Railway lo inyecta) | `8080` |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos, separados por coma | `https://tallerproxy-fronted.vercel.app` |
| `APP_PAYMENT_FAILURE_RATE` | Tasa de fallas simuladas de Pagos | `0.10` |
| `APP_MIN_LATENCY_MS` / `APP_MAX_LATENCY_MS` | Latencia simulada por operación | `15` / `120` |

## Patrón Proxy

```
Controller ──► MicroserviceProxy<T>   (Subject)
                    ▲            ▲
                    │            │
            LoggingProxy<T>   Real service
             (Proxy)          (Inventory / Order / Payment)
                 │
                 └── guarda LogEntry en LogRepository (inicio, duración, resultado/error)
```

- `MicroserviceProxy<T>` declara `T execute(String operation, Object... params)`.
- `LoggingProxy<T>` recibe el servicio real por constructor, genera un `requestId` (UUID), mide con
  `System.nanoTime()`, guarda el `LogEntry` (con copia profunda y enmascarado de datos sensibles) y
  **relanza la misma excepción**.
- Los servicios reales solo se instancian en `ProxyConfig` (raíz de composición); nunca se exponen sin proxy.
- **Proxy sobre proxy**: `OrderService.create` reserva stock a través del proxy de Inventario, por lo que
  la llamada en cascada también queda registrada.

Otros patrones aplicados: **Repository** (`LogRepository` / `InMemoryLogRepository`) y **Strategy**
(`FailureInjector` / `RandomFailureInjector`).

## Endpoints

Base: `/api`. Errores en formato RFC 7807 (`ProblemDetail`).

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| POST | `/api/services/inventory/{operation}` | Operaciones de inventario | 200, 400, 404, 422, 500 |
| POST | `/api/services/orders/{operation}` | Operaciones de pedidos | 200, 400, 404, 422, 500 |
| POST | `/api/services/payments/{operation}` | Operaciones de pagos (10 % de fallas) | 200, 400, 404, 422, 503, 500 |
| GET | `/api/metrics/summary` | Totales, tasa de error y promedio por servicio | 200 |
| GET | `/api/metrics/logs` | Logs filtrados y paginados | 200, 400 |
| POST | `/api/metrics/simulate-load` | Genera 50 llamadas aleatorias | 200 |
| GET | `/actuator/health` | Estado del servicio | 200 |

Operaciones disponibles:

| Servicio | `serviceId` | Operaciones |
|---|---|---|
| Inventario | `inventory-service` | `list`, `check-stock`, `reserve`, `release`, `restock` |
| Pedidos | `order-service` | `create`, `get`, `list`, `cancel` |
| Pagos | `payment-service` | `charge`, `refund`, `status` |

### Ejemplos

```bash
curl -X POST http://localhost:8080/api/services/inventory/reserve \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-001","quantity":2}'

curl -X POST http://localhost:8080/api/services/payments/charge \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-1","amount":120.5}'

curl "http://localhost:8080/api/metrics/logs?service=payment-service&status=ERROR&page=0&size=20"

curl -X POST http://localhost:8080/api/metrics/simulate-load
```

Respuesta de `GET /api/metrics/summary`:

```json
{
  "totalCalls": 120,
  "errorCalls": 14,
  "errorRate": 11.67,
  "avgResponseTimeMs": 63.4,
  "services": [
    {
      "serviceId": "payment-service",
      "totalCalls": 40,
      "successCalls": 34,
      "errorCalls": 6,
      "successRate": 85.0,
      "errorRate": 15.0,
      "avgResponseTimeMs": 71.2,
      "degraded": false
    }
  ]
}
```

`degraded` es `true` cuando `errorRate > 15` (estrictamente mayor).

Respuesta de `GET /api/metrics/logs`:

```json
{
  "content": [
    {
      "requestId": "b3b7c0de-...",
      "serviceId": "payment-service",
      "operation": "charge",
      "durationMs": 87,
      "status": "ERROR",
      "timestamp": "2026-10-30T15:04:05.123Z",
      "input": [ { "orderId": "ORD-1", "amount": 120.5 } ],
      "response": null,
      "errorMessage": "Simulated failure in payment-service while executing 'charge'",
      "stackTrace": [ "com...SimulatedFailureException: ...", "at ..." ]
    }
  ],
  "page": 0, "size": 20, "totalElements": 6, "totalPages": 1
}
```

## Modelo del log

| Campo | Tipo | Notas |
|---|---|---|
| `requestId` | UUID v4 | Único por llamada |
| `serviceId` | String | `inventory-service`, `order-service`, `payment-service` |
| `operation` | String | Validada con `^[a-z0-9-]{1,40}$` |
| `durationMs` | long | Medido con `System.nanoTime()` |
| `status` | enum | `SUCCESS` / `ERROR` |
| `timestamp` | Instant (UTC) | Inicio de la llamada |
| `input` | List | Parámetros enmascarados (`password`, `token`, `secret`, `card`, `cvv`) |
| `response` | Object | Solo si `SUCCESS` |
| `errorMessage` | String | Solo si `ERROR` |
| `stackTrace` | List | Excepción + primeros 5 frames + frames restantes |

## Decisiones de diseño

- **Repository** para poder cambiar el almacenamiento en memoria por una base de datos sin tocar el proxy.
- **Strategy** en `FailureInjector` para que la política del 10 % sea intercambiable y testeable con una
  fuente aleatoria inyectada.
- **Proxy sobre proxy** en Pedidos → Inventario para registrar también las llamadas en cascada.
- Almacén de logs **acotado** (2000 entradas) y sincronizado; métricas calculadas desde el repositorio
  (fuente única de verdad).
- CORS restringido por variable de entorno, solo a `/api/**` y métodos `GET`, `POST`, `OPTIONS`.

## Despliegue (Railway)

El repositorio incluye un `Dockerfile` multi-etapa (build con Maven, runtime con JRE 21 y usuario no root).

1. Crear el servicio desde el repositorio `tallerproxyPatrones-backend`.
2. Configurar `APP_CORS_ALLOWED_ORIGINS` con la URL de Vercel.
3. Generar dominio público y verificar `GET /actuator/health` → `UP`.
4. Probar `POST /api/metrics/simulate-load` → `{"requested":50,...}`.

URL de producción: _pendiente de configurar_.
