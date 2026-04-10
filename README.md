<div align="center">

# ⚡ PulsePoint

### Real-time telemetry ingestion, threshold alerting, and time-series analytics — for anything that produces data.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Maven](https://img.shields.io/badge/Maven-build-red?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

</div>

---

## The problem

You have something generating numbers. A vehicle pushing speed and battery readings every second. A server reporting CPU and memory. A sensor emitting temperature. You need three things:

1. **Store every reading** with timestamps
2. **Fire an alert automatically** when a value crosses a threshold
3. **Query what happened** — raw history, or aggregated stats over a time window

You don't want to stand up Prometheus + Grafana + Alertmanager for a side project. You want one Spring Boot app, one Postgres database, and a REST API you actually understand end to end.

That's PulsePoint.

---

## What it does

```
Any source             PulsePoint                        You query
──────────────         ──────────────────────────────    ─────────────────────────
Vehicle   ──POST──▶   Ingest endpoint                    GET /latest
Server    ──POST──▶   Store DataPoint                    GET /data?from=&to=
Sensor    ──POST──▶   Evaluate alert rules  ──────────▶  GET /alerts?resolved=false
                      Fire Alert if violated              PATCH /alerts/3/resolve
                      Return saved DataPoint              GET /summary → avg/min/max
```

- Register any source — vehicle, server, sensor, payment terminal, anything
- Push readings individually or in batches
- Define threshold rules per source per metric (`speed > 100 = HIGH alert`)
- Alert engine runs automatically on **every single ingest** — no cron, no polling
- Query raw history, latest values, or aggregate statistics over any time window
- Filter, view, and resolve alerts

---

## Tech stack

| | |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3 |
| **Database** | PostgreSQL |
| **ORM** | Spring Data JPA + Hibernate |
| **Build** | Maven |
| **Containerization** | Docker + docker-compose |
| **Utilities** | Lombok, Jakarta Bean Validation |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                           REST Layer                            │
│    SourceController     IngestController     AlertController    │
└──────────┬──────────────────────┬───────────────────┬──────────┘
           │                      │                   │
┌──────────▼──────────────────────▼───────────────────▼──────────┐
│                          Service Layer                          │
│    SourceService         IngestService         AlertService     │
│                               │                                 │
│                      ┌────────▼────────┐                        │
│                      │  Alert Engine   │  ◀── fires on every    │
│                      │ evaluateRules() │      single ingest     │
│                      └────────┬────────┘                        │
└──────────────────────────────┬─────────────────────────────────┘
                               │
┌──────────────────────────────▼─────────────────────────────────┐
│                       Repository Layer                          │
│   SourceRepo    DataPointRepo    AlertRuleRepo    AlertRepo      │
└──────────────────────────────┬─────────────────────────────────┘
                               │
                       ┌───────▼───────┐
                       │  PostgreSQL   │
                       └───────────────┘
```

---

## Project structure

```
src/main/java/com/pulsepoint/
│
├── controller/
│   ├── SourceController.java       POST /api/sources, GET /api/sources, GET /api/sources/{id}
│   ├── IngestController.java       ingest, batch, history, latest, summary endpoints
│   └── AlertController.java        rules and alert lifecycle endpoints
│
├── service/
│   ├── SourceService.java          source registration and lookup
│   ├── IngestService.java          ← the core — ingestion pipeline + alert evaluation engine
│   └── AlertService.java           rule management, alert filtering, resolve lifecycle
│
├── repository/
│   ├── SourceRepository.java
│   ├── DataPointRepository.java    JPQL aggregation queries (avg, min, max, count)
│   ├── AlertRuleRepository.java    findBySourceAndMetricAndActiveTrue
│   └── AlertRepository.java
│
├── model/
│   ├── Source.java                 @Entity → sources table
│   ├── DataPoint.java              @Entity → data_points table (compound indexed)
│   ├── AlertRule.java              @Entity → alert_rules table
│   ├── Alert.java                  @Entity → alerts table
│   └── Summary.java                plain class — aggregation results, never persisted
│
└── enums/
    ├── RuleOperator.java           GT · LT · GTE · LTE · EQ
    └── Severity.java               LOW · MEDIUM · HIGH · CRITICAL
```

---

## Getting started

Two ways to run PulsePoint. Docker is the recommended path — one command, no manual setup. The manual path is there if you prefer running directly in IntelliJ.

---

### 🐳 Option A — Docker (recommended)

**Requires:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) — nothing else.

**1 — Clone and configure**

```bash
git clone https://github.com/yourusername/pulsepoint.git
cd pulsepoint
```

Copy the environment template and fill in your password:

```bash
cp .env.example .env
```

Open `.env` — it looks like this:

```env
POSTGRES_DB=pulsepoint
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password_here   # ← change this
```

**2 — Start everything**

```bash
docker-compose up --build
```

Docker pulls PostgreSQL, builds your app, creates the database, and wires everything together. Watch for:

```
pulsepoint-db   | database system is ready to accept connections
pulsepoint-app  | Started PulsepointApplication in 4.1 seconds
```

**`http://localhost:8090` is live.** No Java installation needed. No database setup needed. No credential configuration beyond the `.env` file.

**Useful commands**

```bash
docker-compose up --build -d      # run in background
docker-compose logs -f            # follow logs
docker-compose down               # stop (data is preserved)
docker-compose down -v            # stop + wipe database completely
docker-compose up --build         # after any code change, always use --build
```

---

### ☕ Option B — Manual (IntelliJ + local PostgreSQL)

**Requires:** Java 17+, PostgreSQL installed locally, Maven (bundled with IntelliJ).

**1 — Create the database**

```sql
CREATE DATABASE pulsepoint;
```

**2 — Set your credentials**

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pulsepoint
spring.datasource.username=postgres
spring.datasource.password=your_password
```

**3 — Run**

```bash
mvn spring-boot:run
```

Or hit the green ▶ button in IntelliJ. Spring creates all four tables automatically on first boot.

```
Started PulsepointApplication in 3.2 seconds (JVM running for 3.8)
```

**`http://localhost:8090` is live.**

---

### 🧪 Testing the API

Regardless of which setup path you used, open `pulsepoint-tester.html` in any browser — no server needed, just double-click the file. It covers every endpoint with pre-filled example values and formatted JSON responses.

Follow the tabs in order: **Sources → Ingest → Alert Rules → Alerts** — and you'll have exercised the entire system in under five minutes.

---

## API reference

### Sources

| Method | Endpoint | What it does |
|---|---|---|
| `POST` | `/api/sources` | Register a new source |
| `GET` | `/api/sources` | List all registered sources |
| `GET` | `/api/sources/{id}` | Get one source by ID |

```http
POST /api/sources
Content-Type: application/json

{
  "name": "Ather-450X-007",
  "type": "VEHICLE",
  "description": "Test vehicle on Bangalore route"
}
```

```jsonc
// Response — 201 Created
{
  "id": 1,
  "name": "Ather-450X-007",
  "type": "VEHICLE",
  "description": "Test vehicle on Bangalore route",
  "active": true,
  "registeredAt": "2024-06-01T10:00:00",
  "lastSeenAt": null          // updates automatically on every ingest
}
```

---

### Ingestion

| Method | Endpoint | What it does |
|---|---|---|
| `POST` | `/api/ingest/{sourceId}` | Push one reading |
| `POST` | `/api/ingest/{sourceId}/batch` | Push multiple readings at once |

**Single reading**

```http
POST /api/ingest/1
Content-Type: application/json

{
  "metric": "speed",
  "value": 72.5,
  "unit": "kmh"
}
```

Leave out `timestamp` and the server timestamps it now. Include it to load historical data.

**Batch**

```http
POST /api/ingest/1/batch
Content-Type: application/json

[
  { "metric": "speed",       "value": 68.0,  "unit": "kmh"     },
  { "metric": "battery",     "value": 43.5,  "unit": "%"       },
  { "metric": "temperature", "value": 81.2,  "unit": "celsius" }
]
```

Every item in the batch runs through the full pipeline independently — stored, rule-evaluated, alerts fired if triggered.

---

### Querying data

| Method | Endpoint | What it does |
|---|---|---|
| `GET` | `/api/sources/{sourceId}/latest` | Most recent reading per metric |
| `GET` | `/api/sources/{sourceId}/data` | All readings in a time range, newest first |
| `GET` | `/api/sources/{sourceId}/summary` | Avg, min, max, count over a window |

**Latest readings** — useful for a live dashboard

```http
GET /api/sources/1/latest
```

**Historical range**

```http
GET /api/sources/1/data?metric=speed&from=2024-06-01T09:00:00&to=2024-06-01T10:00:00
```

**Summary statistics**

```http
GET /api/sources/1/summary?metric=speed&from=2024-06-01T09:00:00&to=2024-06-01T10:00:00
```

```json
{
  "metric": "speed",
  "average": 74.3,
  "minimum": 45.0,
  "maximum": 125.0,
  "totalReadings": 312,
  "from": "2024-06-01T09:00:00",
  "to": "2024-06-01T10:00:00"
}
```

---

### Alert rules

| Method | Endpoint | What it does |
|---|---|---|
| `POST` | `/api/sources/{sourceId}/rules` | Add a threshold rule |
| `GET` | `/api/sources/{sourceId}/rules` | List all rules for a source |

```http
POST /api/sources/1/rules
Content-Type: application/json

{
  "metric": "speed",
  "operator": "GT",
  "threshold": 100,
  "severity": "HIGH"
}
```

| Operators | | Severity levels | |
|---|---|---|---|
| `GT` | greater than `>` | `LOW` | informational |
| `LT` | less than `<` | `MEDIUM` | worth watching |
| `GTE` | greater than or equal `>=` | `HIGH` | needs attention |
| `LTE` | less than or equal `<=` | `CRITICAL` | act immediately |
| `EQ` | equals `=` | | |

---

### Alerts

| Method | Endpoint | What it does |
|---|---|---|
| `GET` | `/api/alerts` | List alerts — all filters optional |
| `PATCH` | `/api/alerts/{alertId}/resolve` | Mark an alert as resolved |

```http
GET /api/alerts
GET /api/alerts?resolved=false
GET /api/alerts?severity=HIGH
GET /api/alerts?sourceId=1&severity=CRITICAL&resolved=false

PATCH /api/alerts/3/resolve
```

Sets `resolvedAt` to the current timestamp. An alert where `resolvedAt` is `null` is open. Once resolved it cannot be reopened — this is intentional.

---

## How the alert engine works

This is the part worth understanding. Inside `IngestService.ingest()`, synchronously, after every write:

```
 Incoming reading  →  source=1 · metric="speed" · value=125.0
          │
          ▼
 Fetch active rules where source=1 AND metric="speed"
          │
          ├── Rule: speed GT 100  severity=HIGH      →  125.0 > 100.0  = TRUE   → 🔴 Alert created
          ├── Rule: speed GT 150  severity=CRITICAL  →  125.0 > 150.0  = false  → skip
          └── Rule: speed LT  30  severity=MEDIUM    →  125.0 < 30.0   = false  → skip
          │
          ▼
 Alert saved:
 {
   metric: "speed",
   triggeredValue: 125.0,
   severity: "HIGH",
   triggeredAt: "2024-06-01T10:31:00",
   resolvedAt: null
 }
          │
          ▼
 DataPoint returned to caller — alerts already committed to DB
```

Key design decisions:

- Only rules matching **both** the source and the exact metric name are evaluated. Ingesting battery touches zero speed rules.
- Severity is **copied from the rule at fire time** — changing a rule's severity later has no effect on past alerts.
- The `rule_id` on an Alert is **nullable** — rules can be deleted without orphaning historical alerts.
- Everything is **synchronous** — by the time the ingest response returns, all alerts are committed. No eventual consistency to reason about in V1.

---

## Database schema

```sql
sources
  id            BIGSERIAL   PRIMARY KEY
  name          VARCHAR
  type          VARCHAR
  description   VARCHAR
  active        BOOLEAN
  registered_at TIMESTAMP
  last_seen_at  TIMESTAMP

data_points
  id        BIGSERIAL   PRIMARY KEY
  source_id BIGINT      REFERENCES sources(id)   NOT NULL
  metric    VARCHAR
  value     DOUBLE PRECISION
  unit      VARCHAR
  timestamp TIMESTAMP
  ─────────────────────────────────────────────────────
  INDEX (source_id, metric, timestamp)
  └─ compound index — keeps time-range and aggregation
     queries O(log n) as the table grows

alert_rules
  id        BIGSERIAL   PRIMARY KEY
  source_id BIGINT      REFERENCES sources(id)   NOT NULL
  metric    VARCHAR
  operator  VARCHAR     stored as "GT" / "LT" / etc.
  threshold DOUBLE PRECISION
  severity  VARCHAR     stored as "HIGH" / etc.
  active    BOOLEAN

alerts
  id              BIGSERIAL   PRIMARY KEY
  source_id       BIGINT      REFERENCES sources(id)       NOT NULL
  rule_id         BIGINT      REFERENCES alert_rules(id)   NULLABLE
  metric          VARCHAR
  triggered_value DOUBLE PRECISION
  severity        VARCHAR
  triggered_at    TIMESTAMP
  resolved_at     TIMESTAMP   NULL = open  ·  NOT NULL = resolved
```

---

## Roadmap

| Version | What | Where it plugs in |
|---|---|---|
| **V1** ✅ | Core REST API — ingest, alert engine, time-series queries, analytics | Initial build — Spring Boot + PostgreSQL |
| **V2** 🟢 **`current`** | Docker + docker-compose — one-command startup, containerized PostgreSQL, `.env` config | Wraps the existing app — zero code changes |
| **V3** | Spring Security — API key auth per source, sources can only push to their own ID | Filter layer in front of all ingest endpoints |
| **V4** | Redis — cache `getLatestReadings()` per source invalidated on every ingest, rate limiting on ingest endpoint | Sits between IngestService and DataPointRepository |
| **V5** | Apache Kafka — ingest endpoint publishes to a topic, consumer handles storage and rule evaluation async | Replaces the direct `dataPointRepository.save()` call in `IngestService.ingest()` |
| **V6** | Observability — expose `/actuator/metrics`, Prometheus scraping, Grafana dashboards | PulsePoint monitors other systems — it should monitor itself |

---

## Use cases

The backend code changes nothing between these. Only source names and metric names differ.

| Domain | Sources | Metrics |
|---|---|---|
| **EV / IoT** | Vehicles | speed, battery %, motor temp, estimated range |
| **DevOps** | Servers | CPU %, memory usage, disk I/O, response time |
| **Healthcare** | Patient monitors | heart rate, blood pressure, SpO2, respiratory rate |
| **Finance** | Payment terminals | transaction amount, failure rate, processing latency |
| **Logistics** | Delivery vehicles | GPS location, fuel level, idle time, delivery status |
| **Smart home** | Appliances | power draw, door state, room humidity, ambient light |

---

> If you use PulsePoint as a reference or build on top of it, a star ⭐ .