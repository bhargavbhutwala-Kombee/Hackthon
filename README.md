# 🚀 Orderly — Hackathon 2.0 Production-Grade App

A full-stack, production-grade ordering platform built for the **Kombee Hackathon 2.0**, demonstrating real-world engineering skills across Application Development, Docker, and — most importantly — **Observability**.

> **Stack:** Java 17 · Spring Boot 3.2 · PostgreSQL · React + Vite · Docker Compose · Prometheus · Loki · Tempo · Grafana

---

## 📐 Architecture

```
Browser                Docker Network (hackthon_default)
   │
   ├─► :4000  orderly-frontend  (React + Nginx)
   │         └─► /api/*  → proxy to app:8080
   │
   ├─► :8080  orderly-app       (Spring Boot REST API)
   │         └─► orderly-postgres:5432
   │
   └─► :3000  orderly-grafana   (Dashboards)
              ├─ orderly-prometheus :9090   (Metrics)
              ├─ orderly-loki      :3100   (Logs via Promtail)
              └─ orderly-tempo     :3200   (Traces via OTLP)
```

---

## ⚡ Quick Start (Docker — no local Java or Node needed)

```powershell
# From the project root
docker compose up -d --build
```

Wait ~30 seconds for the backend health check, then open:

| Service | URL | Credentials |
|---|---|---|
| 🎨 **Frontend** | http://localhost:4000 | Register an account |
| 🔧 **Backend API** | http://localhost:8080/api | JWT via `/auth/login` |
| 📊 **Grafana** | http://localhost:3000 | `admin` / `admin` |
| 📈 **Prometheus** | http://localhost:9090 | — |
| 🔍 **Tempo** | http://localhost:3200 | — |

```powershell
# Stop everything
docker compose down

# Stop and wipe all data
docker compose down -v
```

---

## 🎨 Frontend (React + Vite)

A sleek **dark-mode glassmorphism** UI accessible at **[http://localhost:4000](http://localhost:4000)**.

**Features:**
- 🔐 **Register / Login** with JWT authentication
- 📦 **Product Management** — paginated table with SKU/Name search, Create, Edit, Delete via modal dialogs
- 🔁 **Nginx Reverse Proxy** — routes `/api/*` to the backend seamlessly (no CORS issues)

**Tech:** React 18 · TypeScript · Vite · Tailwind CSS v3 · Axios · React Router v6 · Lucide Icons

---

## 🔧 Backend API (Spring Boot)

**Base URL:** `http://localhost:8080/api`

### Authentication

| Method | Path | Auth Required | Description |
|---|---|---|---|
| `POST` | `/auth/register` | ❌ | Register with `username`, `email`, `password` |
| `POST` | `/auth/login` | ❌ | Login → returns `{ "token": "..." }` |

### Products

| Method | Path | Auth Required | Description |
|---|---|---|---|
| `GET` | `/products` | ✅ | List (params: `page`, `size`, `name`, `sku`) |
| `GET` | `/products/{id}` | ✅ | Get by ID |
| `POST` | `/products` | ✅ | Create (`sku`, `name`, `description`, `price`, `stockQuantity`) |
| `PUT` | `/products/{id}` | ✅ | Update |
| `DELETE` | `/products/{id}` | ✅ | Delete |

### Orders

| Method | Path | Auth Required | Description |
|---|---|---|---|
| `POST` | `/orders` | ✅ | Create (`{ "items": [{ "productId", "quantity" }] }`) |
| `GET` | `/orders` | ✅ | List my orders (params: `page`, `size`, `status`) |
| `GET` | `/orders/{id}` | ✅ | Get by ID |
| `PATCH` | `/orders/{id}` | ✅ | Update status: `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

All errors use a standard shape: `{ "error", "message", "path", "timestamp", "details" }`.

---

## 📊 Observability Stack

### Metrics → Prometheus + Grafana
Prometheus scrapes two sources:
- **Backend** (`/api/actuator/prometheus`) — HTTP request metrics, HikariCP DB pool, JVM memory, custom counters (`auth_login_attempts_total`, `orders_created_total`, etc.)
- **Frontend** (`nginx-exporter:9113`) — Nginx active connections, request rate

### Logs → Promtail → Loki → Grafana
**Promtail** auto-discovers all Docker containers via the Docker socket and ships their logs to Loki with labels (`container`, `service`).

Log sources collected:
- `orderly-app` — Spring Boot structured JSON logs (includes `traceId` for correlation)
- `orderly-frontend` — Nginx access logs
- `orderly-postgres` — PostgreSQL server logs

### Traces → OpenTelemetry → Tempo → Grafana
Every HTTP request creates a root span. Custom child spans are added inside `ProductService` and `OrderService` to trace the full journey.

### 📺 Pre-Provisioned Grafana Dashboards

| Dashboard | Data Source | What it Shows |
|---|---|---|
| **App Health** | Prometheus | RPM, Error Rate %, p95 Latency, Slowest Endpoints |
| **Database Performance** | Prometheus | HikariCP pool usage, slow query detection |
| **Logs** | Loki | Error/Warn log streams, auth failures |
| **Traces** | Tempo | Slow traces (>500ms), span timing breakdown |
| **Unified Logs** | Loki | Backend + Frontend + PostgreSQL logs in one view |
| **System Overview** | Prometheus | Backend AND Frontend metrics side-by-side |

---

## 🕷️ Anomaly Injection

The built-in `AnomalyInjector` artificially degrades performance to demonstrate observability under stress.

Controlled via environment variable in `docker-compose.yml`:
```yaml
ANOMALY_ENABLED: "true"
```

Or tuned via `application.yml`:
```yaml
anomaly:
  enabled: true
  latency-ms: 500           # Adds 500ms delay
  error-probability: 0.1    # 10% chance of returning HTTP 500
  error-endpoints: "/api/products,/api/orders"
```

When enabled, you will see the **p95 latency spike** and **error rate climb** on the App Health dashboard.

---

## 🔥 Load Testing with k6

The `load-test.js` script simulates concurrent users peaking at **100 VUs** to stress-test the system and trigger anomalies.

### Run (Docker — no k6 install needed)

```powershell
docker run --rm -v "c:\Users\Kombee\Desktop\Hackthon:/app" grafana/k6 run /app/load-test.js
```

### Load Stages

```
0s ──────── 30s ──────────── 90s ─────────── 120s ──── 150s
Warm-up      Steady State       SPIKE          Cool-down
0 → 10 VUs   Hold 20 VUs    20 → 100 VUs    100 → 0 VUs
```

### What it Tests

1. **List Products** — `GET /products` with pagination
2. **Search Products** — `GET /products?name=Widget`
3. **Create Product** — `POST /products` (write load)
4. **Get Order** — `GET /orders/{id}` (read with random ID)
5. **List Orders** — `GET /orders`

### Thresholds (Pass/Fail Criteria)

| Threshold | Target |
|---|---|
| `http_req_duration p(95)` | < 2000ms |
| `http_req_failed` | < 10% error rate |
| `product_list_duration p(90)` | < 1500ms |

> 📊 While the test runs, open the **App Health** dashboard in Grafana to watch latency and error rate spike in real-time!

---

## 🗂️ Project Structure

```
Hackthon/
├── src/                         # Spring Boot Java backend
│   └── main/java/com/kombee/orderly/
│       ├── config/              # OpenTelemetry, Security, CORS
│       ├── controller/          # REST controllers
│       ├── service/             # Business logic + custom spans
│       ├── repository/          # JPA repositories
│       └── util/AnomalyInjector.java
├── frontend/                    # React + Vite frontend
│   ├── src/
│   │   ├── pages/               # Login, Register, Products
│   │   ├── AuthContext.tsx       # JWT state management
│   │   └── api.ts               # Axios client
│   ├── nginx.conf               # SPA routing + /stub_status
│   └── Dockerfile               # Multi-stage build → Nginx
├── docker/
│   ├── prometheus.yml           # Scrape configs (backend + nginx)
│   ├── promtail.yml             # Docker log auto-discovery
│   ├── tempo.yml                # Trace storage config
│   └── grafana/provisioning/
│       ├── datasources/         # Prometheus, Loki, Tempo
│       └── dashboards/          # 6 JSON dashboard definitions
├── load-test.js                 # k6 load test script
├── docker-compose.yml           # Full stack orchestration
└── Dockerfile                   # Maven build → JRE image
```

---

## 🛠️ Requirements

- **Docker** + **Docker Compose** (for the full stack — recommended)
- **Java 17+** + **Maven 3.8+** (for local backend-only development)
- **Node 20+** + **npm** (for local frontend-only development)

---

## 🧑‍💻 Local Development (without Docker)

**Backend only:**
```powershell
mvn spring-boot:run
# API at http://localhost:8080/api (uses H2 in-memory by default)
```

**Frontend only:**
```powershell
cd frontend
npm install
# Set your API URL (ensure the backend is running)
$env:VITE_API_URL = "http://localhost:8080/api"
npm run dev
# UI at http://localhost:5173
```
