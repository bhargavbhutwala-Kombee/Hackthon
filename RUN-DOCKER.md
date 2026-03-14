# Run Everything on Docker (No Local Java/Maven)

You only need **Docker Desktop** (or Docker Engine + Docker Compose). Nothing else installed locally.

---

## 1. Prerequisites

- **Docker Desktop for Windows** installed and running.  
  [Download](https://www.docker.com/products/docker-desktop/) if needed.
- Open **PowerShell** or **Command Prompt** and go to the project folder:
  ```powershell
  cd c:\Users\Kombee\Desktop\Hackthon
  ```

---

## 2. Build and start all services

From the project root (`Hackthon` folder), run:

```powershell
docker compose up -d --build
```

- **`--build`** builds the Java app image (Maven runs inside Docker).
- **`-d`** runs everything in the background.

First run can take a few minutes (downloads base images and builds the app).

---

## 3. Check that everything is running

List containers:

```powershell
docker compose ps
```

You should see something like:

| Name              | Status   | Ports                    |
|-------------------|----------|--------------------------|
| orderly-app       | Up       | 0.0.0.0:8080->8080/tcp   |
| orderly-postgres  | Up       | 0.0.0.0:5432->5432/tcp   |
| orderly-prometheus| Up       | 0.0.0.0:9090->9090/tcp   |
| orderly-loki      | Up       | 0.0.0.0:3100->3100/tcp   |
| orderly-tempo     | Up       | 0.0.0.0:3200, 4317, 4318 |
| orderly-grafana   | Up       | 0.0.0.0:3000->3000/tcp   |

If the **app** is still starting, wait 30–60 seconds and check again. View app logs:

```powershell
docker compose logs -f app
```

Press `Ctrl+C` to stop following logs.

---

## 4. Use the application

| What        | URL                          |
|------------|------------------------------|
| **API**    | http://localhost:8080/api     |
| **Health** | http://localhost:8080/api/actuator/health |
| **Grafana**| http://localhost:3000         | Login: **admin** / **admin** |
| **Prometheus** | http://localhost:9090     |

**Quick test:**

1. **Register:**  
   `POST http://localhost:8080/api/auth/register`  
   Body (JSON): `{"username":"test","email":"test@test.com","password":"test123"}`

2. **Login:**  
   `POST http://localhost:8080/api/auth/login`  
   Body: `{"usernameOrEmail":"test","password":"test123"}`  
   Use the returned **token** in the next requests.

3. **List products (with auth):**  
   `GET http://localhost:8080/api/products`  
   Header: `Authorization: Bearer <your-token>`

Or use **Postman**, **Insomnia**, or the **Rest Client** extension in VS Code.

**Pre-seeded user** (if your app has a data initializer): **demo** / **demo123**.

---

## 5. Stop everything

```powershell
docker compose down
```

Data in Postgres, Prometheus, Loki, Tempo, and Grafana is kept in Docker volumes. To remove volumes too (fresh start):

```powershell
docker compose down -v
```

---

## 6. Troubleshooting

**App exits or unhealthy**

- Check logs: `docker compose logs app`
- Ensure Postgres is up: `docker compose ps` (postgres should be “Up” and healthy).
- Wait a bit after `up`; the app needs time to start and run migrations.

**Port already in use**

- Change the host port in `docker-compose.yml`, e.g. `"8081:8080"` for the app.

**Rebuild after code changes**

```powershell
docker compose up -d --build
```

Only the `app` service is rebuilt; other services reuse existing images.
