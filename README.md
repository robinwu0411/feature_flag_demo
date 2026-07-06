# Feature Management Service

A feature flag platform for e-commerce, managing thousands of flags across 100+ applications. SDK evaluates flags in-process (P99 < 1ms); server syncs configs via Redis-backed API. Flags are scoped per application — each app's cache is isolated, so modifying a flag only invalidates that app's Redis entry.

## Architecture

```
                       ┌──────────────────────────────┐
                       │     SDK (Java Library)       │
                       │  ┌──────────┐ ┌───────────┐  │
                       │  │ FlagCache│ │RuleEvaluat│  │
                       │  │(Concur-  │ │ or(enabled│  │
                       │  │ rentMap) │ │  ? → def) │  │
                       │  └──────────┘ └───────────┘  │
                       │  ┌──────────┐                │
                       │  │ Poller   │ P99 eval <1ms  │
                       │  │ (10s,    │ pure in-proc   │
                       │  │  retry)  │                │
                       │  └──────────┘                │
                       └──────────┬───────────────────┘
                                  │ GET /api/v1/eval/sync
                                  ▼
┌───────────────────────────────────────────────────────────────┐
│         Feature Flag Server (Spring Boot, :8080)              │
│                                                               │
│  ┌───────────────────┐  ┌───────────────────────────────┐     │
│  │  Management API   │  │          Sync API             │     │
│  │  CRUD flags       │  │  GET /api/v1/eval/sync        │     │
│  │                   │  │  ?since= (incremental)        │     │
│  └──────┬────────────┘  └───────────┬───────────────────┘     │
│         │ evict                     │ get/set                 │
│         ▼                           ▼                         │
│  ┌──────────────┐           ┌───────────────┐                 │
│  │    MySQL     │           │     Redis     │                 │
│  │   (flag)     │           │   TTL 24h     │                 │
│  └──────────────┘           │ flags::{app}  │                 │
│                             └───────────────┘                 │
│                                                               │
│  Startup: warmup loads all flags from MySQL into Redis        │
│  Observability: Prometheus metrics + structured JSON logs     │
└───────────────────────────────────────────────────────────────┘
                                  │
                  ┌───────────────┼───────────────┐
                  ▼               ▼               ▼
           Prometheus (:9090)  Grafana (:3000)  Swagger (:8080/docs)
```

## Caching Strategy

Two-tier cache: SDK local (evaluation) + Redis (sync). All sync requests hit Redis first; MySQL is only queried on cache miss.

| Tier | Location | Technology | TTL | Purpose |
|------|----------|-----------|-----|---------|
| **SDK** | In-process | ConcurrentHashMap | Driven by sync interval | P99 < 1ms evaluation, zero network calls |
| **Server** | Redis | RedisTemplate (JSON) | 24h | Absorb all sync requests; ~100% hit rate |

### Sync Flow

```
SDK sync ?appId=X&since=T
     │
     ▼
FlagCacheService.getFlagsByApp(appName)   ← Redis GET flags::{appName}
     │
     ├─ Redis HIT  → return cached flags (99.9% of requests)
     │
     └─ Redis MISS → MySQL: WHERE app_name=? AND enabled=TRUE
                     → write to Redis (TTL 24h)
     │
     ▼
SyncService filters by since in Java:
  · since=null  → return all flags for this app
  · since=T1    → return flags for this app where updatedAt > T1
```

### Cache Invalidation

Admin writes call `cacheService.evict(appName)` — only that app's Redis key is deleted. Other apps' caches remain intact. Modifying a flag in app-A never impacts app-B's cache. SDK picks up changes within one sync interval (<=10s).

### Cache Warmup

`CacheWarmup` loads all enabled flags in one query, groups by `app_name`, serializes to JSON and writes to Redis via `RedisTemplate`. The first SDK sync request for any app always hits Redis — no cold-start penalty.

### SDK Resilience

- **Startup:** exponential backoff retry (1s -> 2s -> 4s -> 8s -> 16s, max 5 attempts)
- **Runtime:** continues using last-known-good cache on server failure
- **Cold start:** returns caller-provided default values when cache is empty

## Full API Design

### Management API (`/api/v1/admin/*`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/flags` | Create a feature flag |
| `GET` | `/flags` | List all flags |
| `GET` | `/flags/{id}` | Get flag detail |
| `PUT` | `/flags/{id}` | Update flag |
| `DELETE` | `/flags/{id}` | Delete flag |

### Sync API (used by SDK)

| Request | Description |
|---------|-------------|
| `GET /api/v1/eval/sync?appId=X` | Full sync — returns all enabled flags for app X |
| `GET /api/v1/eval/sync?appId=X&since=<ISO timestamp>` | Incremental sync — returns flags changed since timestamp |

Response format:

```json
{
  "flags": [
    {
      "key": "new_checkout_ui",
      "type": "BOOLEAN",
      "defaultValue": "false",
      "enabled": true,
      "releaseVersion": "v3.2.0",
      "updatedAt": "2026-07-06T05:32:27Z"
    }
  ],
  "serverTime": "2026-07-06T06:30:00.000Z"
}
```

| Field | Description |
|-------|-------------|
| `flags[]` | Array of FlagConfig objects (enabled flags only) |
| `flags[].key` | Unique flag identifier |
| `flags[].type` | BOOLEAN / NUMBER / STRING |
| `flags[].defaultValue` | Fallback value if disabled, or configured value if enabled |
| `flags[].enabled` | Whether the flag is currently active |
| `flags[].releaseVersion` | Release version this flag is associated with |
| `flags[].updatedAt` | Last modification timestamp (used for incremental sync `since`) |
| `serverTime` | Server's current time (SDK uses as next `since` parameter) |

### Health API

`GET /api/v1/health` — Database + Redis connectivity check
`GET /actuator/prometheus` — Prometheus metrics endpoint

## Explainability Model

Every flag evaluation produces an `EvalResult`:

```
is it enabled?           → value, reason (enabled / disabled)
For whom?                → userId
In which region?         → region
Which release?           → releaseVersion
When was config updated? → flagUpdatedAt
Why did this happen?     → trace[]
```

### How the SDK Works

The SDK is a Java library embedded in your application. There is no HTTP server — you call Java methods directly in your business code.

**Startup:** the SDK creates a `FeatureFlagClient`, which spawns a background `ConfigPoller` thread. The poller calls `GET /api/v1/eval/sync?appId=X` every 10s to pull the latest flag configs from the server into a local `ConcurrentHashMap`. All subsequent evaluations read from this local cache with zero network calls (P99 < 1ms).

**Example:** your e-commerce checkout code calls `client.isEnabled("new_checkout_ui", user)` to decide which UI to render. No HTTP request happens — the SDK reads the flag from its local cache and evaluates it in-process.

**Resilience:** if the server goes down, the SDK keeps using its last-known-good cache. If the SDK starts before the server is ready, it retries with exponential backoff (1s → 2s → 4s → 8s → 16s).

### SDK Interface

```java
// 1. Init (once at app startup)
FeatureFlagClient client = FeatureFlagClient.builder()
        .serverUrl("http://flag-server:8080")
        .appId("my-app")
        .syncInterval(Duration.ofSeconds(10))
        .build();
client.start();

// 2. Build user context
FFUser user = FFUser.builder()
    .id("user_123")
    .region("eu-west")
    .plan("premium")
    .custom("device_type", "mobile")
    .build();

// 3. Evaluate flags — pure local, no network call
if (client.isEnabled("new_checkout_ui", user)) {
    renderNewUI();
}
String  theme = client.stringValue("theme_color", user, "light");
int     max   = client.intValue("max_search_results", user, 20);

// 4. Batch evaluation with explainability
Map<String, EvalResult> results = client.evaluateAll(user, "flag_a", "flag_b");
// EvalResult: flagKey, value, reason, userId, region, releaseVersion, flagUpdatedAt, trace[]
```

## Observability

### Server Metrics (Micrometer -> Prometheus)

| Metric | Type | Description |
|--------|------|-------------|
| `ff_sync_requests_total` | Counter | Total sync endpoint calls |
| `ff_sync_duration_ms` | Timer | Sync endpoint latency |
| `ff_admin_operations_total` | Counter | Admin write operations |
| `ff_admin_read_operations_total` | Counter | Admin read operations |
| `ff_admin_write_duration_seconds` | Timer | Admin write latency with percentiles |
| `ff_health_checks_total` | Counter | Health check endpoint calls |

### Prometheus

Prometheus scrapes metrics from the server every 15 seconds. Access at `http://localhost:9090`.

**Example queries** (enter in the search bar → Execute):

```
ff_sync_requests_total              # Total sync requests
rate(ff_sync_requests_total[5m])    # Sync request rate
ff_sync_duration_ms_seconds_max     # Max sync latency
ff_health_checks_total              # Health check count
```

### Grafana

Pre-configured dashboard with all feature flag metrics. Access at `http://localhost:3000`.

| Field | Value |
|-------|-------|
| URL | `http://localhost:3000` |
| Username | `admin` |
| Password | `admin` |

The **"Feature Flag Platform"** dashboard (left sidebar → Dashboards) shows 7 panels:

| Panel | Type | What it shows |
|-------|------|---------------|
| Sync Requests | Stat | Requests per second |
| Sync Duration Max | Stat | Maximum sync latency |
| Sync Duration Avg | Stat | Rolling average latency |
| Admin Operations | Graph | Write/read rate over time |
| Admin Write Duration | Graph | p50/p95/p99 latency over time |
| Health Checks | Stat | Total health check calls |
| Sync Request Count | Stat | Cumulative sync requests |

### Structured Logging

JSON-formatted logs with `requestId` for correlation:
```json
{"ts":"2026-07-05 16:07:44.314","level":"INFO","logger":"...","msg":"...","requestId":"796caa6e53"}
```

### Request Tracing

Every response includes an `X-Request-Id` header. Incoming `X-Request-Id` is propagated; otherwise a new one is generated.

## Testing

### Automated Tests

```bash
# Run all unit/integration tests (12 total)
mvn test

# Server tests (7): H2 in-memory DB + mocked Redis
# SDK tests (5): pure unit tests, no external dependencies
```

**What each test covers:**

| Module | Test | What it verifies |
|--------|------|-----------------|
| Server | `FlagServiceTest` (4) | Flag CRUD: create, list, update, delete |
| Server | `SyncControllerTest` (2) | Sync API: full sync returns flags filtered by app; other apps return empty |
| Server | `ApplicationTests` (1) | Spring context loads successfully |
| SDK | `RuleEvaluatorTest` (5) | Disabled→default; BOOLEAN enabled→true; non-BOOLEAN enabled→defaultValue; releaseVersion in result; trace produced |

### Manual Integration Test (Docker)

Start everything and walk through the cache flow — ideal for live demo.

**Setup**

```bash
mvn clean package -DskipTests
docker compose down -v
docker compose up -d
```

---

#### 1. Health check (wait 5s after docker container starts)

```bash
curl http://localhost:8080/api/v1/health
```

Expected: `{"database":"UP","redis":"UP"}`

#### 2. Verify warmup: when springboot starts, it loads flags from mysql to redis

```bash
docker compose logs server | grep -i warmup
```

Expected:
```
CacheWarmup bean created
Starting cache warmup...
Cache warmup: app=sample-app, flags=3
Cache warmup complete
```

#### 3. Sync — Redis HIT (warmup loaded cache, no MySQL SQL)

```bash
curl -s "http://localhost:8080/api/v1/eval/sync?appId=sample-app" | jq '.flags | length'
```

Expected: `3`

```bash
docker compose logs server --tail 4
```

Expected: no `FlagMapper` SQL lines — data came from Redis

#### 4. Incremental sync

```bash
curl -s "http://localhost:8080/api/v1/eval/sync?appId=sample-app&since=2099-01-01T00:00:00Z" | jq
```

Expected: `{"flags":[],"serverTime":"..."} No incremental flags at this moment`

#### 5. SDK evaluation — curl sample app, then check logs

```bash
curl -s "http://localhost:8081/demo/checkout?userId=user_1&region=eu-west&plan=premium" | jq
```

Then check the sample app logs to see what the SDK evaluated:

```bash
docker compose logs sample --tail 10
```

Expected logs:

```
Checkout [user_1, eu-west, premium]: new_checkout_ui=true, dark_mode=true, max_search_results=20
Explain [new_checkout_ui]: value=true, reason=enabled, userId=user_1, region=eu-west, release=v3.2.0
Explain [dark_mode]: value=true, reason=enabled, userId=user_1, region=eu-west, release=v3.1.0
Explain [max_search_results]: value=20, reason=enabled, userId=user_1, region=eu-west, release=v3.0.0
```

The sample app calls `FeatureFlagClient` (pure Java, no HTTP) and logs every evaluation result.

#### 6. Create flag → Redis eviction → MySQL MISS

```bash
curl -s -X POST http://localhost:8080/api/v1/admin/flags \
  -H "Content-Type: application/json" \
  -d '{"key":"demo_'$(date +%s)'","name":"Demo","flagType":"BOOLEAN","defaultValue":"true","enabled":true,"appName":"sample-app","createdBy":"admin"}' | jq '{id,key,appName}'
```

Expected: `{"id":...,"key":"demo_...","appName":"sample-app"}`

Sync again — this time cache MISS, hits MySQL:

```bash
curl -s "http://localhost:8080/api/v1/eval/sync?appId=sample-app" | jq '.flags | length'
```

Expected: `4`

```bash
docker compose logs server --tail 6
```

Expected: `FlagMapper.findEnabledByAppName` with `SELECT * FROM flag WHERE enabled = TRUE AND app_name = 'sample-app'`

#### 7. SDK local cache survives server outage

```bash
docker compose stop server
curl -s "http://localhost:8081/demo/checkout?userId=user_1" | jq '.new_checkout_ui'
```

Expected: `true` (evaluates from local ConcurrentHashMap, server is down)

```bash
docker compose start server
```

#### 8. SDK retry with exponential backoff (1s → 2s → 4s → 8s → 16s)

Stop both containers, then start sample while server is still down:

```bash
docker compose stop server sample
docker compose start sample
```

Wait ~5s for sample to boot and SDK to begin retrying, then check logs:

```bash
docker compose logs sample | grep "SDK"
```

Since server is down, the SDK retries with exponential backoff:

```
SDK sync attempt 1/5 failed, retrying in 1000ms
SDK sync attempt 2/5 failed, retrying in 2000ms
SDK sync attempt 3/5 failed, retrying in 4000ms
SDK sync attempt 4/5 failed, retrying in 8000ms
```

Now start the server:

```bash
docker compose start server
docker compose logs sample | grep "SDK"
```

Once the server is up, the next attempt succeeds:

```
SDK initial sync: 4 flags loaded
```

The SDK gives up after 5 attempts (1+2+4+8+16 ≈ 31s total), then the scheduled poller retries every 10s. If the server comes back within either window, the SDK reconnects automatically.

## Sample App API

The sample app (`:8081`) is a demo Spring Boot application that embeds the SDK. It shows how to evaluate flags in your business code — no HTTP calls, pure in-process evaluation.

### DemoRunner (startup)

Runs on startup, evaluates 3 flags against a demo user, and prints results to logs. Check it with:

```bash
docker compose logs sample | grep -E "\[new_checkout_ui\]|\[dark_mode\]|\[max_search_results\]|Explain"
```

### DemoController (HTTP endpoints)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/demo/checkout?userId=&region=&plan=` | Simulate a checkout request — evaluates 3 flags and returns results with full explainability |
| `GET` | `/demo/explain/{flagKey}?userId=&region=&plan=` | Get detailed explainability for a single flag (value, reason, releaseVersion, trace) |

**Example:**

```bash
curl -s "http://localhost:8081/demo/checkout?userId=user_1&region=eu-west&plan=premium" | jq
```

Response:

```json
{
  "user_id": "user_1",
  "region": "eu-west",
  "plan": "premium",
  "new_checkout_ui": true,
  "dark_mode": true,
  "max_search_results": 20,
  "explainability": {
    "new_checkout_ui": { "flagKey": "new_checkout_ui", "value": "true", "reason": "enabled", ... },
    "dark_mode": { "flagKey": "dark_mode", "value": "true", "reason": "enabled", ... },
    "max_search_results": { "flagKey": "max_search_results", "value": "20", "reason": "enabled", ... }
  }
}
```

Single flag explainability:

```bash
curl -s "http://localhost:8081/demo/explain/new_checkout_ui?userId=user_1&region=eu-west" | jq
```

## Services

| Service | Port | URL |
|---------|------|-----|
| Server | 8080 | `http://localhost:8080` |
| Sample App | 8081 | `http://localhost:8081` |
| Prometheus | 9090 | `http://localhost:9090` |
| Grafana | 3000 | `http://localhost:3000` (admin/admin) |
| Swagger UI | 8080 | `http://localhost:8080/docs` |

## Project Structure

```
sdk-spec/                    Cross-platform SDK interface contract
  ├── sdk-interface.md
  └── test-cases.json         Shared test scenarios
server/                      Spring Boot admin server
  ├── config/                 RequestIdFilter, CacheWarmup
  ├── controller/             HealthController, ManagementController, SyncController
  ├── service/                FlagService, SyncService, FlagCacheService
  ├── model/entity/           Flag (with appName field)
  ├── model/dto/              CreateFlagRequest, FlagDto, SyncResponse
  └── mapper/                 FlagMapper (MyBatis)
sdk/                         Java client library
  ├── FeatureFlagClient       Main entry point (builder pattern)
  ├── cache/FlagCache         Thread-safe ConcurrentHashMap
  ├── sync/ConfigPoller       Periodic sync with retry + exponential backoff
  ├── evaluator/RuleEvaluator Evaluates flag enabled state
  └── model/                  FFUser, FlagConfig, EvalResult, TraceEntry
sample/                      Demo Spring Boot app consuming the SDK
  ├── DemoRunner.java          Logs SDK evaluation results on startup
  └── DemoController.java      HTTP endpoint that triggers SDK evaluation
docker-compose.yml           MySQL + Redis + Server + Sample + Prometheus + Grafana
prometheus.yml               Prometheus scrape configuration
grafana/                     Grafana provisioning
init-db.sql                  MySQL schema + 3 seed flags
```
