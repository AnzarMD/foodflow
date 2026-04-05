# 🍔 FoodFlow — Distributed Food Delivery Platform

A production-grade distributed microservices system built with Java 21, Spring Boot 3.5, React, RabbitMQ, Redis, and WebSocket. Features real-time order status updates pushed to the customer's browser the moment a restaurant owner accepts their order.

![CI](https://github.com/AnzarMD/foodflow/actions/workflows/ci.yml/badge.svg)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        React Frontend                        │
│              Vite · Tailwind · React Query                   │
│         WebSocket (STOMP) ←──── real-time updates           │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP :8080
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                       API Gateway                            │
│         Spring Cloud Gateway · JWT validation               │
│              Routes · CORS · Rate limiting                   │
└──────────┬──────────────────────┬───────────────────────────┘
           │                      │
           ▼                      ▼
┌──────────────────────┐  ┌──────────────────────┐
│   Order Service      │  │  Restaurant Service   │
│   port 8081          │  │  port 8082            │
│   order_db           │  │  restaurant_db        │
│   Auth · JWT         │  │  Menus · Orders       │
└──────────┬───────────┘  └──────────┬────────────┘
           │ RabbitMQ                 │ RabbitMQ
           │ order.placed ──────────▶│
           │                         │ order.accepted ──┐
           │◀────────────────────────────────────────────┘
           │ Redis pub/sub
           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Notification Service                       │
│        port 8083 · WebSocket / STOMP · Redis subscriber     │
│            Pushes status updates to customer browser         │
└─────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 · Vite · Tailwind CSS · React Query · @stomp/stompjs |
| Backend | Java 21 · Spring Boot 3.5.x · Spring Security 6 |
| Gateway | Spring Cloud Gateway 2024.0.x — static routing, no Eureka |
| Database | PostgreSQL 16, DB-per-service pattern |
| Async events | RabbitMQ 3.13 — topic exchange, durable queues, manual acks |
| Real-time | Redis 7 pub/sub → WebSocket / STOMP → Browser |
| Auth | JWT HS256, 15 min access + 7 day rotating refresh token |
| Containers | Docker + Docker Compose |
| CI | GitHub Actions |
| Build | Maven (mvnw wrapper) |

---

## Features

- **JWT authentication** — register, login, refresh token rotation, logout with token revocation
- **Restaurant management** — create restaurants, add menu items, manage incoming orders
- **Order placement** — customers browse restaurants, add to cart, place orders (202 Accepted)
- **Event-driven order flow** — RabbitMQ delivers `OrderPlaced` → restaurant, `OrderAccepted` → order service
- **Real-time notifications** — Redis pub/sub → WebSocket / STOMP → instant browser update, no polling
- **Role-based access** — `CUSTOMER` and `RESTAURANT_OWNER` roles with `@PreAuthorize` enforcement
- **Full Docker stack** — entire backend starts with `docker-compose up --build -d`
- **Integration tests** — `@SpringBootTest` tests for auth and order flows

---

## Quick Start

### Prerequisites

- Docker Desktop (running)
- Node.js 18+
- Java 21 (only needed to run services in IntelliJ — not required for Docker)

### 1. Start the backend

```bash
git clone https://github.com/AnzarMD/foodflow.git
cd foodflow
docker-compose up --build -d
docker-compose ps   # wait for all 7 services to show healthy
```

> First build takes ~5 minutes (Maven downloads dependencies). Subsequent starts are fast.

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

### 3. Create test accounts

Register a **Customer** and a **Restaurant Owner** via the UI at `/register`, or use the pre-seeded test accounts:

| Role | Email | Password |
|---|---|---|
| Customer | `customer@test.com` | `password123` |
| Owner | `owner@test.com` | `password123` |

### 4. Add a restaurant (as owner)

```bash
curl -X POST http://localhost:8080/api/v1/restaurants \
  -H "Authorization: Bearer <owner_token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Restaurant","cuisineType":"Indian","address":"123 MG Road"}'
```

Then add menu items:

```bash
curl -X POST http://localhost:8080/api/v1/restaurants/<restaurant_id>/menu \
  -H "Authorization: Bearer <owner_token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Butter Chicken","price":350,"category":"Main Course"}'
```

---

## Project Structure

```
foodflow/
├── docker-compose.yml            # Full stack orchestration
├── docker/postgres/init.sql      # Creates restaurant_db
├── test-websocket.html           # WebSocket dev test client
├── frontend/                     # React app (Vite)
│   └── src/
│       ├── api/                  # Axios instances + API calls
│       ├── context/              # AuthContext (JWT state)
│       ├── hooks/                # useAuth, useWebSocket
│       ├── pages/                # LoginPage, HomePage, etc.
│       └── components/           # Navbar, ProtectedRoute, etc.
└── services/
    ├── api-gateway/              # Spring Cloud Gateway (8080)
    ├── order-service/            # Auth + Orders (8081)
    ├── restaurant-service/       # Restaurants + Menus (8082)
    └── notification-service/     # WebSocket + Redis (8083)
```

---

## API Reference

### Auth — no token required

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user |
| `POST` | `/api/v1/auth/login` | Login, get tokens |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token |

### Orders — `CUSTOMER` role

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/orders` | Place order (202 Accepted) |
| `GET` | `/api/v1/orders/my` | List my orders |
| `GET` | `/api/v1/orders/{id}` | Get order status |
| `PATCH` | `/api/v1/orders/{id}/cancel` | Cancel pending order |

### Restaurants — public `GET`, `RESTAURANT_OWNER` for mutations

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/restaurants` | List all active restaurants |
| `GET` | `/api/v1/restaurants/{id}/menu` | Get menu items |
| `POST` | `/api/v1/restaurants` | Create restaurant |
| `POST` | `/api/v1/restaurants/{id}/menu` | Add menu item |
| `GET` | `/api/v1/restaurants/incoming-orders` | View pending orders |
| `PATCH` | `/api/v1/restaurants/orders/{id}/accept` | Accept order |
| `PATCH` | `/api/v1/restaurants/orders/{id}/reject` | Reject order |

---

## Running Tests

```bash
# Ensure Docker containers are running first
cd services/order-service
./mvnw test -Dspring.profiles.active=test
```

---

## Key Design Decisions

**Database-per-service** — `order_db` and `restaurant_db` are separate databases. No cross-service foreign keys. Shared data (restaurant name, item name/price) is denormalized at write time.

**Gateway as sole JWT validator** — only the API Gateway validates JWTs. Downstream services trust `X-User-Id` and `X-User-Role` headers injected by the Gateway. Zero JWT code in Restaurant/Notification services.

**RabbitMQ for durable events, Redis for ephemeral push** — `OrderPlaced` and `OrderAccepted` are durable RabbitMQ messages (survive restarts, manually acked). The final WebSocket push uses Redis pub/sub — fire-and-forget, sub-millisecond.

**202 Accepted for order placement** — customers get an immediate response with the order ID. Status updates arrive asynchronously via WebSocket. This is how real food delivery apps work.

---

## License

MIT
