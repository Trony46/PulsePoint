<div align="center">

# ⚡ PulsePoint

### Real-time telemetry ingestion, threshold alerting, and time-series analytics — for anything that produces data.

<br/>

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
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

You don't want to stand up Prometheus + Grafana + Alertmanager for a side project. You want one Spring Boot app, one PostgreSQL database, and a REST API you actually understand end to end — where only the right sources can push data, every reading is tracked, and repeated reads are fast.

That's PulsePoint.

---

## What It Does

```
Any source             PulsePoint                          You query
──────────────         ────────────────────────────────    ──────────────────────────
Vehicle   ──POST──▶   Validate API key                     GET  /latest  ← Redis cached
Server    ──POST──▶   Store DataPoint                      GET  /data?from=&to=
Sensor    ──POST──▶   Evaluate alert rules  ────────────▶  GET  /alerts?resolved=false
                      Fire Alert if violated                PATCH /alerts/3/resolve
                      Evict Redis cache                     GET  /summary → avg/min/max
                      Return saved DataPoint
```

- **Source registration** — register any data source with a name and type, receive a unique API key
- **Authenticated ingestion** — sources push readings using their API key; unauthorized requests are rejected
- **Automatic alerting** — define threshold rules per source per metric; alerts fire on every ingest, no polling
- **Redis caching** — `getLatestReadings()` is cached per source, invalidated on every ingest
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
| **Cache** | Redis 7 — Spring Cache abstraction (`@Cacheable` / `@CacheEvict`) |
| **ORM** | Spring Data JPA + Hibernate |
| **Security** | Spring Security — API key filter + CORS configuration |
| **Containerization** | Docker + docker-compose |
| **Build** | Maven |
| **Utilities** | Lombok, Jakarta Bean Validation |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         Security Layer                           │
│     ApiKeyFilter · CORS config — guards all ingest endpoints     │
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
│                    ┌───────────┤                                  │
│                    │           │                                  │
│            ┌───────▼───────┐  ┌▼──────────────┐                 │
│            │  Redis Cache  │  │  Alert Engine  │ ◀── every ingest│
│            │  @Cacheable   │  │ evaluateRules()│                 │
│            │  @CacheEvict  │  └───────┬────────┘                 │
│            └───────────────┘          │                          │
└───────────────────────────────────────┼─────────────────────────┘
                                        │
┌───────────────────────────────────────▼─────────────────────────┐
│                       Repository Layer                           │
│   SourceRepo     DataPointRepo     AlertRuleRepo     AlertRepo   │
└──────────────────────┬────────────────────────────────────────  ┘
                       │
          ┌────────────┴────────────┐
    ┌─────▼──────┐          ┌───────▼───────┐
    │ PostgreSQL │          │     Redis     │
    │  (persist) │          │    (cache)    │
    └────────────┘          └───────────────┘
```

---

## Project Structure

```
pulsepoint/
│
├── src/main/java/com/pulsepoint/
│   │
│   ├── config/
│   │   └── CacheConfig.java            Redis cache manager · TTL · JSON serialization
│   │
│   ├── controller/
│   │   ├── SourceController.java       POST /api/sources · GET /api/sources · GET /api/sources/{id}
│   │   ├── IngestController.java       ingest · batch · history · latest · summary
│   │   └── AlertController.java        rules · alerts · resolve
│   │
│   ├── service/
│   │   ├── SourceService.java          registration · lookup · API key generation
│   │   ├── IngestService.java          ← core — ingestion + alert engine + cache eviction
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
│   │   └── SecurityConfig.java         CORS rules · session policy · filter registration
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
├── SETUP.md                            ← infrastructure setup guide (Redis · PostgreSQL · Docker)
└── pulsepoint-tester.html
```

---

## Getting Started

Two ways to run PulsePoint. Docker is recommended — one command starts the app, PostgreSQL, and Redis together.

> 📖 For detailed infrastructure setup instructions, troubleshooting, and a Kafka learning roadmap, see **[SETUP.md](SETUP.md)**.

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

PostgreSQL, Redis, and the app all start in the correct order via healthchecks.

```
pulsepoint-redis | Ready to accept connections
pulsepoint-db    | database system is ready to accept connections
pulsepoint-app   | Started PulsepointApplication in 4.1 seconds
```

**`http://localhost:8090` is live.** No Java needed. No PostgreSQL needed. No Redis needed.

**Commands**

```bash
docker-compose up --build -d      # run in background
docker-compose logs -f            # follow live logs
docker-compose down               # stop — data preserved
docker-compose down -v            # stop + wipe all volumes
docker-compose up --build         # always use --build after code changes
```

---

### ☕ Option B — Manual (IntelliJ + Local Services)

> **Requires:** Java 17+, PostgreSQL installed locally, Redis running (see [SETUP.md](SETUP.md)), Maven.

**1 — Start Redis (quickest way)**

```bash
docker run -d --name pulsepoint-redis -p 6379:6379 redis:7-alpine
```

**2 — Create the PostgreSQL database**

```sql
CREATE DATABASE pulsepoint;
```

**3 — Configure credentials**

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pulsepoint
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**4 — Run**

```bash
mvn spring-boot:run
```

Or hit ▶ in IntelliJ. **`http://localhost:8090` is live.**

---

### 🧪 Testing the API

Open `pulsepoint-tester.html` in any browser — double-click it, no server needed. Every endpoint is covered with pre-filled example values, formatted JSON responses, and an API key field on all ingest calls.

**Follow this order:** Sources → Ingest → Alert Rules → Alerts

> ⚠️ When you register a source, the `apiKey` is returned **once only**. The tester auto-captures it into the global key bar at the top. It is used automatically on all ingest requests.

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
  "apiKey": "a3f8c2d1-7b4e-4c1a-9f3d-2e8b1c6a5d9f",   // ← save this — shown once
  "registeredAt": "2024-06-01T10:00:00",
  "lastSeenAt": null
}
```

---

### Ingestion

> 🔒 **Requires** `X-Api-Key` header matching the source ID in the URL.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ingest/{sourceId}` | Push one reading |
| `POST` | `/api/ingest/{sourceId}/batch` | Push multiple readings at once |

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

Every ingest also **evicts the Redis cache** for that source so the next `/latest` call reflects the new reading.

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

---

### Querying Data

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/sources/{sourceId}/latest` | Most recent reading per metric — **Redis cached** |
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

---

## How The Alert Engine Works

Inside `IngestService.ingest()`, synchronously, after every write:

```
 Incoming → source=1 · metric="speed" · value=125.0
       │
       ├─ @CacheEvict("latest-readings", key=1) ← Redis cache cleared first
       │
       ▼
 Fetch active rules WHERE source=1 AND metric="speed"
       │
       ├── speed GT 100  HIGH      →  125.0 > 100.0  = TRUE   → 🔴 Alert created
       ├── speed GT 150  CRITICAL  →  125.0 > 150.0  = false  → skip
       └── speed LT  30  MEDIUM    →  125.0 < 30.0   = false  → skip
       │
       ▼
 DataPoint returned — alert committed · cache evicted
```

**Design decisions:**
- Only rules matching **both** the source and the exact metric name are evaluated
- Severity is **copied from the rule at fire time** — changing a rule later has no effect on past alerts
- `rule_id` on Alert is **nullable** — rules can be deleted without orphaning historical alerts
- Everything is **synchronous** — alerts and cache eviction are committed before the response returns

---

## Database Schema

```sql
sources
  id            BIGSERIAL   PRIMARY KEY
  name          VARCHAR
  type          VARCHAR
  description   VARCHAR
  api_key       VARCHAR     unique per source — authenticates ingest requests
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

**17 new files. 0 files modified.**

| File | Role |
|---|---|
| `model/Source.java` | `@Entity` — registered data sources |
| `model/DataPoint.java` | `@Entity` — individual metric readings, compound indexed |
| `model/AlertRule.java` | `@Entity` — threshold rules per source per metric |
| `model/Alert.java` | `@Entity` — fired alert records |
| `model/Summary.java` | plain class — holds aggregation results, not persisted |
| `enums/RuleOperator.java` | GT · LT · GTE · LTE · EQ |
| `enums/Severity.java` | LOW · MEDIUM · HIGH · CRITICAL |
| `repository/SourceRepository.java` | JPA basic CRUD |
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
Server-managed fields (`id`, `active`, `registeredAt`, `lastSeenAt`) are marked READ_ONLY. Jackson includes them in responses but ignores them on input. This removes the need for a separate DTO layer entirely.

**Key design decision — compound index on `data_points`**
`INDEX (source_id, metric, timestamp)` keeps time-range queries at O(log n) as the table grows.

**Key design decision — `resolvedAt` null means open**
`resolvedAt: null` = open, `resolvedAt: <timestamp>` = resolved. Eliminates a redundant boolean field and encodes resolution time for free.

**Key design decision — synchronous alert evaluation**
`evaluateAlertRules()` runs inside `ingest()` before returning. Alerts are committed to the database before the caller gets the 201.

</details>

---

<details>
<summary><strong>V2 — Docker + docker-compose</strong> &nbsp;✅&nbsp; <em>Zero-setup deployment</em></summary>

<br/>

### What
Containerized the application and PostgreSQL so the entire system starts with `docker-compose up --build`. Added `.env.example` for credential configuration and a two-stage Dockerfile for a lean production image.

### Why
Without Docker, running PulsePoint required five manual steps: install Java 17, install PostgreSQL, create the database, edit `application.properties`, then run. Any version mismatch or port conflict meant debugging the environment before debugging the project.

Docker collapses this to: clone → fill `.env` → one command. No Java needed. No PostgreSQL needed. The contributor is hitting `localhost:8090` in two minutes.

A second reason: Docker is the correct next step *before* adding Redis and Kafka. Without it, every new infrastructure dependency requires another manual install on every machine. Containerizing now means future services arrive pre-wired.

### How

**3 new files. 1 modified.**

| File | What it does |
|---|---|
| `Dockerfile` | Two-stage build — Maven compiles the jar, JRE-Alpine runs it. Final image contains only the jar. |
| `docker-compose.yml` | `postgres` (port 5433 external) + `app` (port 8090). App waits for postgres healthcheck. Named volume persists data. |
| `.env.example` | Credential template — copy to `.env`, set password, `.env` is gitignored. |
| `application.properties` | Environment variable fallbacks: `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/pulsepoint}` |

**Key decision — port 5433 for PostgreSQL externally**
Avoids collision with any locally installed PostgreSQL on 5432. App container talks to postgres on 5432 internally via Docker DNS.

**Key decision — healthcheck before app start**
`condition: service_healthy` makes the app container wait until `pg_isready` returns OK. Prevents the crash-on-startup race condition.

**Key decision — dependency layer caching**
`COPY pom.xml` + `RUN mvn dependency:go-offline` before `COPY src`. Docker caches the dependency layer. Code changes don't re-download dependencies — rebuilds are fast.

</details>

---

<details>
<summary><strong>V3 — API Key Authentication + CORS</strong> &nbsp;✅&nbsp; <em>Security</em></summary>

<br/>

### What
Secured the ingest endpoints with per-source API key authentication. When a source registers, the system generates a UUID key returned once. Every ingest must include that key in the `X-Api-Key` header matching the source ID in the URL.

Also added full CORS configuration so the `pulsepoint-tester.html` file can call the API when opened directly from the filesystem (`file:///`).

All read endpoints remain open. Only write endpoints are protected.

### Why
Without authentication, PulsePoint's ingest endpoints were fully public:

- Anyone could push fabricated readings and corrupt time-series history
- Deliberate threshold crossings would flood the alerts table with false positives
- A script hitting `/api/ingest/1` thousands of times per second could crash the database

The root problem: there was no way to verify that the caller of `/api/ingest/1` was actually source 1.

JWT was not chosen — it solves human user authentication (login sessions, passwords). PulsePoint has no users, only sources. A UUID key issued at registration and validated per-request is the simplest mechanism that closes the actual attack surface.

CORS was needed because browsers block `fetch()` calls from `file:///` to `localhost` by default. Without the CORS config, the HTML tester couldn't call the API at all.

### How

**2 new files. 4 modified.**

| File | What changed |
|---|---|
| `security/ApiKeyFilter.java` | `OncePerRequestFilter` — passes non-ingest paths through, validates `X-Api-Key` on ingest paths, 401 or 403 on failure |
| `security/SecurityConfig.java` | CORS rules (`setAllowedOriginPatterns("*")`, all methods, all headers including `X-Api-Key`). Disables session management. Registers `ApiKeyFilter`. |
| `model/Source.java` | Added `apiKey` field `@JsonProperty(READ_ONLY)` |
| `repository/SourceRepository.java` | Added `Optional<Source> findByApiKey(String apiKey)` |
| `service/SourceService.java` | `source.setApiKey(UUID.randomUUID().toString())` in `createSource()` |
| `pom.xml` | Added `spring-boot-starter-security` |

**Filter validation flow:**
```
POST /api/ingest/1
  → path starts with /api/ingest/? YES
  → X-Api-Key header present?      NO  → 401 Missing header
                                   YES
  → key exists in DB?              NO  → 401 Invalid key
                                   YES
  → key belongs to source 1?       NO  → 403 Key mismatch
                                   YES → request reaches controller
```

**Key decision — 401 vs 403**
401 = "I don't know who you are." 403 = "I know who you are, you're just not allowed here." A source using its own valid key on a different source's endpoint gets 403, not 401.

**Key decision — UUID key shown once**
`apiKey` is READ_ONLY for Jackson input but included in output. It appears exactly once — in the 201 on registration. No retrieval endpoint exists. If lost, re-register. This is exactly how Stripe and GitHub handle API keys.

**Key decision — CORS allows all origins with `setAllowedOriginPatterns("*")`**
`setAllowedOrigins("*")` doesn't work when `allowCredentials` is true, but since we're not using credentials/cookies, the pattern approach works for all origins including `file:///`.

</details>

---

<details>
<summary><strong>V4 — Redis Caching</strong> &nbsp;🟢&nbsp; <code>current</code></summary>

<br/>

### What
Added Redis as a caching layer for `getLatestReadings()`. The result is cached per source ID after the first call and invalidated automatically on every ingest. Cache entries expire after 10 minutes as a safety net. Values are stored as human-readable JSON in Redis.

### Why
`getLatestReadings()` is the most frequently called read endpoint — every live dashboard polls it. Without caching it runs multiple database queries every single call: one to get distinct metric names, then one per metric to find the most recent reading.

Under load (multiple dashboards, high ingest frequency) this becomes the primary bottleneck. The data also changes slowly — it only changes when a new ingest arrives. Serving repeated calls from RAM instead of running the same PostgreSQL queries is the correct optimization.

The cache invalidation strategy is exact: `@CacheEvict` on `ingest()` deletes only the cache entry for the source that just ingested. Other sources' caches are untouched.

### How

**2 new files. 4 modified.**

| File | What changed |
|---|---|
| `config/CacheConfig.java` | `RedisCacheManager` bean — 10-minute TTL, `RedisSerializer.json()` for readable JSON storage |
| `PulsepointApplication.java` | Added `@EnableCaching` |
| `service/IngestService.java` | `@CacheEvict(value="latest-readings", key="#sourceId")` on `ingest()` · `@Cacheable(value="latest-readings", key="#sourceId")` on `getLatestReadings()` |
| `application.properties` | `spring.data.redis.host` and `spring.data.redis.port` with local fallbacks |
| `docker-compose.yml` | Added `redis` service (port 6379) with healthcheck · app `depends_on` redis healthy · `SPRING_REDIS_HOST: redis` injected |
| `pom.xml` | Added `spring-boot-starter-data-redis` |

**How the cache works at runtime:**
```
First call:  getLatestReadings(1) → cache MISS → runs DB queries → stores in Redis → returns
Next N calls: getLatestReadings(1) → cache HIT → returns from Redis (0.1ms) → DB never called

ingest(1, ...) fires:  @CacheEvict deletes "latest-readings::1" from Redis
Next call:   getLatestReadings(1) → cache MISS → DB queries run again → re-caches
```

**Key decision — `RedisSerializer.json()` over `GenericJackson2JsonRedisSerializer`**
`RedisSerializer.json()` is the modern Spring Data Redis API — cleaner import, same behaviour. Stores values as readable JSON so you can inspect cached data directly with `redis-cli GET "latest-readings::1"` and see actual JSON, not binary garbage.

**Key decision — 10-minute TTL**
Even if `@CacheEvict` fails to fire for some reason, cache entries auto-expire. This prevents stale data being served indefinitely. The TTL is a safety net, not the primary invalidation mechanism.

**Key decision — Redis runs in Docker only**
Redis is not installed locally. For IntelliJ runs, start a Redis container separately with one command: `docker run -d --name pulsepoint-redis -p 6379:6379 redis:7-alpine`. The `application.properties` fallback to `localhost:6379` connects to it automatically.

</details>

---

<details>
<summary><strong>V5 — Apache Kafka</strong> &nbsp;⏳&nbsp; <em>planned</em></summary>

<br/>

### What
Move alert evaluation off the ingest thread. The ingest endpoint publishes the DataPoint to a Kafka topic and returns immediately. A separate consumer handles storage and rule evaluation asynchronously.

### Why
In V1–V4 the ingest endpoint is synchronous — it stores the reading, evaluates every matching rule, writes any alerts, evicts the cache, and only then responds. Under high ingest frequency this blocks the caller for the full evaluation time. Kafka decouples the write from the evaluation so ingest latency drops to a single publish call.

### How
`ingest()` publishes to a `data-points` Kafka topic instead of calling `dataPointRepository.save()` directly. A `@KafkaListener` consumer handles the save and the `evaluateAlertRules()` call. Existing service logic is unchanged — it just runs in a different thread. See [SETUP.md](SETUP.md) for the Kafka learning roadmap before implementing this version.

</details>

---

<details>
<summary><strong>V6 — Observability</strong> &nbsp;⏳&nbsp; <em>planned</em></summary>

<br/>

### What
Expose `/actuator/metrics` via Spring Boot Actuator. Scrape with Prometheus. Visualize ingest rate, alert fire rate, cache hit ratio, and query latency in Grafana dashboards.

### Why
PulsePoint monitors other systems. It should monitor itself. Without observability you are blind to performance degradation, cache effectiveness, and alert engine throughput.

### How
Add `spring-boot-starter-actuator` and `micrometer-registry-prometheus`. Expose `/actuator/prometheus`. Add Prometheus and Grafana to `docker-compose.yml`. Import a pre-built dashboard for ingest throughput and alert volume.

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

Built as part of a backend engineering learning path — Spring Boot · PostgreSQL · Redis · Docker · Spring Security · Real-time data systems.

<br/>

*If you use PulsePoint as a reference or build on top of it, a star ⭐ is appreciated.*

</div>