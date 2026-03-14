# Orderly Backend (Java)

Spring Boot backend for the Kombee Hackathon: registration, login, Products & Orders CRUD with pagination/filtering, validation, OpenTelemetry spans, structured logging, and configurable anomaly injection.

## Requirements

- Java 17+
- Maven 3.8+

## Build & Run

```bash
mvn spring-boot:run
```

API base: `http://localhost:8080/api`

## Main endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST   | `/auth/register` | No  | Register (username, email, password, displayName) |
| POST   | `/auth/login`    | No  | Login (usernameOrEmail, password) → JWT |
| GET    | `/products`      | Yes | List products (page, size, name, sku) |
| GET    | `/products/{id}` | Yes | Get product |
| POST   | `/products`      | Yes | Create product |
| PUT    | `/products/{id}` | Yes | Update product |
| DELETE | `/products/{id}` | Yes | Delete product |
| POST   | `/orders`        | Yes | Create order (body: `{ "items": [ { "productId", "quantity" } ] }`) |
| GET    | `/orders`        | Yes | List my orders (page, size, status) |
| GET    | `/orders/{id}`   | Yes | Get order by id |

## Demo user (after first run)

- **Username:** `demo`
- **Password:** `demo123`

## Observability

- **Metrics:** `GET /api/actuator/prometheus`
- **Health:** `GET /api/actuator/health`
- **Logs:** JSON-capable via `logback-spring.xml` (use profile `observability` or `prod` for JSON).
- **Traces:** Manual spans in controllers and services; OpenTelemetry SDK is initialized (add OTLP exporter later for Tempo).

## Anomaly injection

In `application.yml`:

```yaml
anomaly:
  enabled: true
  latency-ms: 500
  error-probability: 0.1
  error-endpoints: "/api/products,/api/orders"
```

Enables artificial delay and random 500s on product/order endpoints for observability demos.
