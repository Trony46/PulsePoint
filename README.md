<div align="center">

# ⚡ PulsePoint

### Real-time telemetry ingestion, threshold alerting, and time-series analytics — for anything that produces data.

<br/>

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-API%20Key-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Maven](https://img.shields.io/badge/Maven-build-red?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

</div>

---

## The Problem

You have something generating numbers. A vehicle pushing speed and battery readings every second. A server reporting CPU and memory. A sensor emitting temperature.

You need three things:

1. **Store every reading** with a timestamp
2. **Fire an alert automatically** when a value crosses a threshold
3. **Query what happened** — raw history, or aggregated stats over a time window

You don't want to stand up Prometheus + Grafana + Alertmanager for a side project. You want one Spring Boot app, one PostgreSQL database, and a REST API you actually understand end to end — where only the right sources can push data, and every reading is tracked.

That's PulsePoint.

---

## What It Does

```
Any source             PulsePoint                          You query
──────────────         ────────────────────────────────    ──────────────────────────
Vehicle   ──POST──▶   Validate API key                     GET  /latest
Server    ──POST──▶   Store DataPoint                      GET  /data?from=&to=
Sensor    ──POST──▶   Evaluate alert rules  ────────────▶  GET  /alerts?resolved=false
                      Fire Alert if violated                PATCH /alerts/3/resolve
                      Return saved DataPoint                GET  /summary → avg/min/max
```

- **Source registration** — register any data source with a name and type, receive a unique API key
- **Authenticated ingestion** — sources push readings using their API key; unauthorized requests are rejected
- **Automatic alerting** — define threshold rules per source per metric; alerts fire on every ingest, no polling
- **Time-series queries** — historical readings for any source and metric within a time range
- **Analytics** — average, minimum, maximum, and count for any metric over any window
- **Alert lifecycle** — filter open alerts by severity or source, mark them resolved

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3 |
| **Database** | PostgreSQL 16 |
| **ORM** | Spring Data JPA + Hibernate |
| **Security** | Spring Security — API key filter |
| **Containerization** | Docker + docker-compose |
| **Build** | Maven |
| **Utilities** | Lombok, Jakarta Bean Validation |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         Security Layer                           │
│              ApiKeyFilter — guards all ingest endpoints          │
└──────────────────────────────┬───────────────────────────────────┘
                               │  authenticated requests only
┌──────────────────────────────▼───────────────────────────────────┐
│                           REST Layer                             │
│    SourceController      IngestController      AlertController   │
└──────────┬───────────────────────┬──────────────────┬───────────┘
           │                       │                  │
┌──────────▼───────────────────────▼──────────────────▼───────────┐
│                          Service Layer                           │
│    SourceService          IngestService          AlertService    │
│                                │                                 │
│                       ┌────────▼────────┐                        │
│                       │  Alert Engine   │  ◀── fires on every    │
│                       │ evaluateRules() │      single ingest     │
│                       └────────┬────────┘                        │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                       Repository Layer                           │
│   SourceRepo     DataPointRepo     AlertRuleRepo     AlertRepo   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                        ┌───────▼───────┐
                        │  PostgreSQL   │
                        └───────────────┘
```

---

## Project Structure

```
pulsepoint/
│
├── src/main/java/com/pulsepoint/
│   │
│   ├── controller/
│   │   ├── SourceController.java       POST /api/sources · GET /api/sources · GET /api/sources/{id}
│   │   ├── IngestController.java       ingest · batch · history · latest · summary
│   │   └── AlertController.java        rules · alerts · resolve
│   │
│   ├── service/
│   │   ├── SourceService.java          registration · lookup · API key generation
│   │   ├── IngestService.java          ← core — ingestion pipeline + alert evaluation engine
│   │   └── AlertService.java           rule management · alert filtering · resolve lifecycle
│   │
│   ├── repository/
│   │   ├── SourceRepository.java       findByApiKey
│   │   ├── DataPointRepository.java    time-range queries · JPQL aggregations
│   │   ├── AlertRuleRepository.java    findBySourceAndMetricAndActiveTrue
│   │   └── AlertRepository.java        findBySource · findAll ordered
│   │
│   ├── model/
│   │   ├── Source.java                 @Entity → sources table
│   │   ├── DataPoint.java              @Entity → data_points table (compound indexed)
│   │   ├── AlertRule.java              @Entity → alert_rules table
│   │   ├── Alert.java                  @Entity → alerts table
│   │   └── Summary.java                plain class — aggregation results, never persisted
│   │
│   ├── security/
│   │   ├── ApiKeyFilter.java           intercepts ingest requests · validates key ownership
│   │   └── SecurityConfig.java         disables session auth · registers filter
│   │
│   └── enums/
│       ├── RuleOperator.java           GT · LT · GTE · LTE · EQ
│       └── Severity.java               LOW · MEDIUM · HIGH · CRITICAL
│
├── src/main/resources/
│   └── application.properties
│
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── pulsepoint-tester.html
```

---

## Getting Started

Two ways to run PulsePoint. Docker is recommended — one command, no manual setup.

---

### 🐳 Option A — Docker (Recommended)

> **Requires:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) — nothing else.

**1 — Clone and configure**

```bash
git clone https://github.com/yourusername/pulsepoint.git
cd pulsepoint
cp .env.example .env
```

Open `.env` and set your password:

```env
POSTGRES_DB=pulsepoint
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password_here
```

**2 — Start everything**

```bash
docker-compose up --build
```

PostgreSQL starts, the app builds, tables are created, the API is live.

```
pulsepoint-db   | database system is ready to accept connections
pulsepoint-app  | Started PulsepointApplication in 4.1 seconds
```

**`http://localhost:8090` is live.** No Java needed. No PostgreSQL needed. No database setup.

**Commands**

```bash
docker-compose up --build -d      # run in background
docker-compose logs -f            # follow live logs
docker-compose down               # stop — data preserved
docker-compose down -v            # stop + wipe database
docker-compose up --build         # always use --build after code changes
```

---

### ☕ Option B — Manual (IntelliJ + Local PostgreSQL)

> **Requires:** Java 17+, PostgreSQL installed locally, Maven (bundled with IntelliJ).

**1 — Create the database**

```sql
CREATE DATABASE pulsepoint;
```

**2 — Configure credentials**

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

Or hit ▶ in IntelliJ. Spring creates all tables on first boot.

**`http://localhost:8090` is live.**

---

### 🧪 Testing the API

Open `pulsepoint-tester.html` in any browser — double-click it, no server needed. Every endpoint is covered with pre-filled example values, formatted JSON responses, and an API key field on ingest.

**Follow this order:** Sources → Ingest → Alert Rules → Alerts

> ⚠️ **V3 note:** When you register a source, copy the `apiKey` from the response immediately. You will need it to send data. Paste it into the X-Api-Key field in the Ingest tab.

---

## API Reference

### Sources

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/sources` | Register a new source — returns an API key |
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
// 201 Created
{
  "id": 1,
  "name": "Ather-450X-007",
  "type": "VEHICLE",
  "active": true,
  "apiKey": "a3f8c2d1-7b4e-4c1a-9f3d-2e8b1c6a5d9f",   // ← save this
  "registeredAt": "2024-06-01T10:00:00",
  "lastSeenAt": null
}
```

> `apiKey` is shown **once** at registration. Store it — there is no way to retrieve it again.

---

### Ingestion

> 🔒 **These endpoints require** `X-Api-Key` header matching the source.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ingest/{sourceId}` | Push one reading |
| `POST` | `/api/ingest/{sourceId}/batch` | Push multiple readings at once |

**Single reading**

```http
POST /api/ingest/1
Content-Type: application/json
X-Api-Key: a3f8c2d1-7b4e-4c1a-9f3d-2e8b1c6a5d9f

{
  "metric": "speed",
  "value": 72.5,
  "unit": "kmh"
}
```

Omit `timestamp` and the server sets it to now. Include it to backfill historical data.

**Batch**

```http
POST /api/ingest/1/batch
Content-Type: application/json
X-Api-Key: a3f8c2d1-7b4e-4c1a-9f3d-2e8b1c6a5d9f

[
  { "metric": "speed",       "value": 68.0,  "unit": "kmh"     },
  { "metric": "battery",     "value": 43.5,  "unit": "%"       },
  { "metric": "temperature", "value": 81.2,  "unit": "celsius" }
]
```

Each item runs through the full pipeline independently — stored, rule-evaluated, alerts fired if triggered.

---

### Querying Data

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/sources/{sourceId}/latest` | Most recent reading per metric |
| `GET` | `/api/sources/{sourceId}/data` | All readings in a time range, newest first |
| `GET` | `/api/sources/{sourceId}/summary` | Avg, min, max, count over a window |

```http
GET /api/sources/1/latest

GET /api/sources/1/data?metric=speed&from=2024-06-01T09:00:00&to=2024-06-01T10:00:00

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

### Alert Rules

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/sources/{sourceId}/rules` | Add a threshold rule to a source |
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

| Operator | Meaning | | Severity | Meaning |
|---|---|---|---|---|
| `GT` | greater than `>` | | `LOW` | informational |
| `LT` | less than `<` | | `MEDIUM` | worth watching |
| `GTE` | greater than or equal `>=` | | `HIGH` | needs attention |
| `LTE` | less than or equal `<=` | | `CRITICAL` | act immediately |
| `EQ` | equals `=` | | | |

---

### Alerts

| Method | Endpoint | Description |
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

`resolvedAt: null` = open alert. Once resolved it cannot be reopened — intentional.

---

## How The Alert Engine Works

Inside `IngestService.ingest()`, synchronously, after every write to the database:

```
 Incoming → source=1 · metric="speed" · value=125.0
       │
       ▼
 Fetch active rules WHERE source=1 AND metric="speed"
       │
       ├── speed GT 100  HIGH      →  125.0 > 100.0  = TRUE   → 🔴 Alert created
       ├── speed GT 150  CRITICAL  →  125.0 > 150.0  = false  → skip
       └── speed LT  30  MEDIUM   →  125.0 < 30.0   = false  → skip
       │
       ▼
 Alert: { triggeredValue: 125.0, severity: HIGH, resolvedAt: null }
       │
       ▼
 DataPoint returned — alerts already committed to DB
```

**Design decisions:**
- Only rules matching **both** the source and the exact metric name are evaluated
- Severity is **copied from the rule at fire time** — changing a rule later has no effect on past alerts
- `rule_id` on Alert is **nullable** — rules can be deleted without orphaning historical alerts
- Everything is **synchronous** — alerts are committed before the response returns

---

## Database Schema

```sql
sources
  id            BIGSERIAL   PRIMARY KEY
  name          VARCHAR
  type          VARCHAR
  description   VARCHAR
  api_key       VARCHAR     unique per source — used to authenticate ingest
  active        BOOLEAN
  registered_at TIMESTAMP
  last_seen_at  TIMESTAMP

data_points
  id        BIGSERIAL   PRIMARY KEY
  source_id BIGINT      REFERENCES sources(id)  NOT NULL
  metric    VARCHAR
  value     DOUBLE PRECISION
  unit      VARCHAR
  timestamp TIMESTAMP
  INDEX (source_id, metric, timestamp)
  └─ compound index — O(log n) time-range and aggregation queries

alert_rules
  id        BIGSERIAL   PRIMARY KEY
  source_id BIGINT      REFERENCES sources(id)  NOT NULL
  metric    VARCHAR
  operator  VARCHAR
  threshold DOUBLE PRECISION
  severity  VARCHAR
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

## Version History

Every version answers three questions: **what** was built, **why** it was needed, and **how** it was implemented.

---

<details>
<summary><strong>V1 — Core REST API</strong> &nbsp;✅&nbsp; <em>Foundation</em></summary>

<br/>

### What
Built the complete backend from scratch — source registration, data ingestion, an automatic alert evaluation engine, time-series history queries, and statistical aggregations. No frontend, no auth, no containers — just the core system working end to end.

### Why
The goal was to answer: can a single Spring Boot application reliably receive high-frequency metric readings, persist them, and automatically evaluate threshold rules on every write? This version proves that answer is yes and establishes the architecture that every future version builds on.

### How

**6 new files added. 0 files modified.**

| File | Role |
|---|---|
| `model/Source.java` | `@Entity` — registered data sources |
| `model/DataPoint.java` | `@Entity` — individual metric readings, compound indexed |
| `model/AlertRule.java` | `@Entity` — threshold rules per source per metric |
| `model/Alert.java` | `@Entity` — fired alert records |
| `model/Summary.java` | plain class — holds aggregation results, not persisted |
| `enums/RuleOperator.java` | GT · LT · GTE · LTE · EQ |
| `enums/Severity.java` | LOW · MEDIUM · HIGH · CRITICAL |
| `repository/SourceRepository.java` | JPA — basic CRUD |
| `repository/DataPointRepository.java` | derived queries + JPQL aggregations (avg, min, max, count) |
| `repository/AlertRuleRepository.java` | `findBySourceAndMetricAndActiveTrue` |
| `repository/AlertRepository.java` | ordered finds by source and globally |
| `service/SourceService.java` | registration, lookup, `findSourceOrThrow` helper |
| `service/IngestService.java` | ingest, batch, history, latest, summary, **evaluateAlertRules()** |
| `service/AlertService.java` | addRule, getRules, getAlerts with filters, resolveAlert |
| `controller/SourceController.java` | POST + GET endpoints for sources |
| `controller/IngestController.java` | ingest, batch, data, latest, summary endpoints |
| `controller/AlertController.java` | rules and alert lifecycle endpoints |

**Key design decision — `@JsonProperty(access = READ_ONLY)`**
Server-managed fields (`id`, `active`, `registeredAt`, `lastSeenAt`) are marked READ_ONLY so clients cannot inject them in request bodies. Jackson includes them in responses but ignores them on input. This removes the need for a separate DTO layer.

**Key design decision — compound index on `data_points`**
`INDEX (source_id, metric, timestamp)` keeps time-range queries at O(log n) as the table grows. Without it, every history or summary query is a full table scan.

**Key design decision — `resolvedAt` null means open**
Instead of a boolean `resolved` flag, `resolvedAt: null` means the alert is open. `resolvedAt: <timestamp>` means resolved. This eliminates a field and encodes the resolution time for free.

**Key design decision — synchronous alert evaluation**
`evaluateAlertRules()` is called inside `ingest()` before returning the response. Alerts are guaranteed committed to the database before the caller gets the 201. No async complexity in V1.

</details>

---

<details>
<summary><strong>V2 — Docker + docker-compose</strong> &nbsp;✅&nbsp; <em>Zero-setup deployment</em></summary>

<br/>

### What
Containerized the application and its PostgreSQL dependency so the entire system starts with one command: `docker-compose up --build`. Added a `.env.example` template for credential configuration and a two-stage Dockerfile for a lean production image.

### Why
Without Docker, running PulsePoint required five manual steps: install Java 17, install PostgreSQL, create the database, edit credentials in `application.properties`, then run the app. Any version mismatch, port conflict, or misconfiguration meant debugging the environment before debugging the project. For anyone wanting to evaluate or contribute to PulsePoint, that friction was a real barrier.

Docker reduces all of that to: clone → fill in `.env` → one command. No Java required. No PostgreSQL required. The contributor is looking at running API responses in two minutes.

A secondary reason: Docker is the correct next step before adding more infrastructure dependencies (Redis, Kafka). Every service added later would require more manual installs without it. Containerizing now means future infrastructure arrives pre-wired.

### How

**3 new files added. 1 file modified.**

| File | What it does |
|---|---|
| `Dockerfile` | Two-stage build — Maven stage compiles the jar, JRE-Alpine stage runs it. Final image contains only the jar, not Maven or source code. |
| `docker-compose.yml` | Defines two services: `postgres` (port 5433 externally to avoid local conflicts) and `app` (port 8090). App depends on postgres with a healthcheck condition — starts only after PostgreSQL is ready to accept connections. Named volume `postgres_data` persists database across restarts. |
| `.env.example` | Credential template — users copy this to `.env`, set their password, and `.env` is gitignored so credentials never reach version control. |
| `application.properties` | Updated to read from environment variables with local fallbacks: `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/pulsepoint}`. Works in Docker (env vars injected) and in IntelliJ (falls back to hardcoded local values). |

**Key decision — port 5433 externally for PostgreSQL**
The compose file maps `5433:5432` so the Dockerized PostgreSQL doesn't collide with any locally installed PostgreSQL running on 5432. The app container talks to it on `5432` internally — Docker's network handles the resolution.

**Key decision — healthcheck before app start**
Without `condition: service_healthy`, the app container starts immediately, tries to connect to PostgreSQL before it's ready, and crashes. The healthcheck polls `pg_isready` every 5 seconds and the app waits until it passes.

**Key decision — dependency caching in Dockerfile**
`COPY pom.xml .` and `RUN mvn dependency:go-offline` happen before `COPY src`. Docker layers are cached — if you change source code but not dependencies, the rebuild skips the dependency download entirely. Rebuilds are significantly faster.

</details>

---

<details>
<summary><strong>V3 — API Key Authentication</strong> &nbsp;🟢&nbsp; <code>current</code></summary>

<br/>

### What
Secured the ingest endpoints with per-source API key authentication. When a source registers, the system generates a UUID key and returns it once. Every subsequent ingest request must include that key in the `X-Api-Key` header. Requests without a valid key, or with a key belonging to a different source, are rejected before they reach any controller.

All read endpoints (`GET /api/sources`, `GET /api/alerts`, etc.) remain open — only the write endpoints that could corrupt data or generate false alerts are protected.

### Why
Without authentication, PulsePoint's ingest endpoints were fully public. Any person who discovered the URL and a source ID could:

- Push fabricated readings and corrupt the time-series history
- Deliberately cross alert thresholds to flood the alerts table with false positives
- Run a script hitting `/api/ingest/1` thousands of times per second and crash the database

The fundamental problem was that there was no way for the system to verify that the thing calling `/api/ingest/1` was actually source 1. A vehicle's own client and a random attacker were indistinguishable.

JWT and OAuth were not chosen because they solve a different problem — authenticating human users who log in with passwords. PulsePoint has no users. It has sources. A UUID API key issued at registration and validated on every ingest is the simplest mechanism that actually closes the attack surface.

### How

**2 new files added. 3 files modified.**

| File | What changed |
|---|---|
| `security/ApiKeyFilter.java` | `OncePerRequestFilter` — intercepts every request, passes non-ingest paths straight through, validates the `X-Api-Key` header on ingest paths, rejects with 401/403 if invalid |
| `security/SecurityConfig.java` | Disables Spring Security's auto-configured login page and session management, registers `ApiKeyFilter` before `UsernamePasswordAuthenticationFilter` |
| `model/Source.java` | Added `apiKey` field, marked `@JsonProperty(READ_ONLY)` |
| `repository/SourceRepository.java` | Added `Optional<Source> findByApiKey(String apiKey)` |
| `service/SourceService.java` | Added `source.setApiKey(UUID.randomUUID().toString())` in `createSource()` |
| `pom.xml` | Added `spring-boot-starter-security` dependency |

**How the filter validates a request — step by step:**

```
Request arrives at: POST /api/ingest/1
                              │
              Does path start with /api/ingest/ ?
                    NO → pass through, untouched
                    YES ↓
              Is X-Api-Key header present?
                    NO → 401 "Missing X-Api-Key header"
                    YES ↓
              Does this key exist in the database?
                    NO → 401 "Invalid API key"
                    YES ↓
              Does the key belong to source ID 1 (from the URL)?
                    NO → 403 "API key does not belong to this source"
                    YES ↓
              filterChain.doFilter() — request reaches the controller
```

The 401 vs 403 distinction is intentional: 401 means "I don't know who you are", 403 means "I know who you are but you're not allowed here." A source sending its own valid key to another source's endpoint gets 403, not 401.

**Key decision — `anyRequest().permitAll()` in SecurityConfig**
This looks like it disables security but it doesn't. The `ApiKeyFilter` runs before Spring Security evaluates this rule. By the time a request reaches `permitAll()`, the filter has already either rejected it or allowed it through. The Spring Security layer exists to suppress the default login-redirect behaviour — the actual gate is the filter.

**Key decision — UUID for key generation**
`UUID.randomUUID()` generates a 128-bit cryptographically random identifier. The probability of two UUIDs colliding is astronomically small. No additional library, no key management service — just `java.util.UUID`. Sufficient for this threat model.

**Key decision — key shown once, never retrievable**
The `apiKey` field is `READ_ONLY` for Jackson but still serialized on output. It appears exactly once: in the 201 response when the source is first created. There is no "retrieve my key" endpoint. This is intentional — if a key is lost, the source re-registers. This mirrors how services like Stripe and GitHub handle API keys.

</details>

---

<details>
<summary><strong>V4 — Redis Caching</strong> &nbsp;⏳&nbsp; <em>planned</em></summary>

<br/>

### What
Cache the `getLatestReadings()` response per source in Redis. Invalidate the cache on every ingest. Add request rate limiting on ingest endpoints.

</details>

---

<details>
<summary><strong>V5 — Apache Kafka</strong> &nbsp;⏳&nbsp; <em>planned</em></summary>

<br/>

### What
Move alert evaluation off the ingest thread. The ingest endpoint publishes the DataPoint to a Kafka topic and returns immediately. A separate consumer handles storage and rule evaluation asynchronously.

</details>

---

<details>
<summary><strong>V6 — Observability</strong> &nbsp;⏳&nbsp; <em>planned</em></summary>

<br/>

### What
Expose `/actuator/metrics` via Spring Boot Actuator. Scrape with Prometheus. Visualize with Grafana dashboards showing ingest rate, alert fire rate, and query latency.

</details>

---

## Use Cases

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

<div align="center">

Built as part of a backend engineering learning path — Spring Boot · PostgreSQL · Docker · Spring Security · Real-time data systems.

<br/>

*If you use PulsePoint as a reference or build on top of it, a star ⭐ is appreciated.*

</div>