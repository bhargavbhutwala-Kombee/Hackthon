# Orderly Backend (Java)

Spring Boot backend for the Kombee Hackathon: registration, login, Products & Orders CRUD with pagination/filtering, validation, OpenTelemetry spans, structured logging, and configurable anomaly injection.

## Quick start (Docker only — no local Java/Maven)

```powershell
cd c:\Users\Kombee\Desktop\Hackthon
docker compose up -d --build
```

Then open **http://localhost:8080/api/actuator/health** and **http://localhost:3000** (Grafana: admin/admin).  
See **[RUN-DOCKER.md](RUN-DOCKER.md)** for full steps and troubleshooting.

---

## Database

**Use PostgreSQL** for the hackathon (production-like, good for DB performance dashboards).

- **Local (no Docker):** Install PostgreSQL, create DB `orderly`, user `orderly`/password `orderly`, then run with profile `docker`:
  ```bash
  set SPRING_PROFILES_ACTIVE=docker
  mvn spring-boot:run
  ```
- **With Docker:** The stack uses PostgreSQL in Docker; no manual DB setup needed (see below).

Default (no profile) uses **H2 in-memory** for quick local runs.

---

## Requirements

- Java 17+
- Maven 3.8+ (for local build)
- Docker & Docker Compose (for full stack)

## Build & Run (local)

```bash
mvn spring-boot:run
```

API base: `http://localhost:8080/api`

## Docker (full stack: App + PostgreSQL + Prometheus + Loki + Tempo + Grafana)

From the project root:

```bash
docker compose up -d --build
```

| Service     | URL                      | Purpose        |
|------------|---------------------------|----------------|
| Application| http://localhost:8080/api | Backend API    |
| PostgreSQL | localhost:5432            | Database       |
| Prometheus | http://localhost:9090     | Metrics        |
| Loki       | http://localhost:3100     | Logs           |
| Tempo      | http://localhost:3200     | Traces         |
| Grafana    | http://localhost:3000     | Dashboards (admin/admin) |

- **Database:** PostgreSQL 16, DB `orderly`, user/password `orderly`. The app uses it automatically when run via Docker (profile `docker`).
- **Traces:** App sends spans to Tempo via OTLP HTTP (`OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4318`).
- **Metrics:** Prometheus scrapes `http://app:8080/api/actuator/prometheus`.
- **Grafana:** Pre-provisioned datasources: Prometheus (default), Loki, Tempo.

Stop everything:

```bash
docker compose down
```

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
| PATCH  | `/orders/{id}`   | Yes | Update order status (body: `{ "status": "CONFIRMED" \| "SHIPPED" \| "DELIVERED" \| "CANCELLED" }`) |

All error responses use a standard shape: `{ "error", "message", "path", "timestamp", "details" }`. Validation and not-found return 400 and 404 accordingly.

## Demo user (after first run)

- **Username:** `demo`
- **Password:** `demo123`

## Observability

- **Metrics:** `GET /api/actuator/prometheus` — includes HTTP request metrics plus custom counters: `auth_login_attempts_total`, `auth_login_failures_total`, `auth_register_total`, `orders_created_total`, `products_created_total`, `validation_failures_total`.
- **Health:** `GET /api/actuator/health` — includes DB status when run with Docker profile.
- **Logs:** Structured JSON in Docker (`docker,observability` profile); `traceId` and `spanId` in MDC for correlation and filtering by trace ID in Loki.
- **Traces:** Every request has a root `http.request` span; controllers and services create child spans. OTLP export to Tempo when `OTEL_EXPORTER_OTLP_ENDPOINT` is set (e.g. in Docker).

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
