# Feature Management Service — Design Spec

## Overview

A feature flag (feature toggle) service for an e-commerce platform managing thousands of flags across 100+ applications. The system serves flag configurations to SDKs, which evaluate flags locally for minimal latency.

**Tech Stack:** Java 17, Spring Boot 3, MySQL, Redis, Docker Compose
**Delivery:** Monorepo — `server/` (SpringBoot service) + `sdk/` (Java library) + `sample/` (demo app using SDK)

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│  Sample App (sample/)  │  Any Java Service  │  ...       │
│  depends on SDK        │  depends on SDK     │            │
│  uses: isEnabled()     │                     │            │
└───────────┬────────────┴──────────┬──────────┴────────────┘
            │                       │
            │  SDK (Java Library, published as JAR)
            │  - Local cache (ConcurrentHashMap)
            │  - Periodic sync (10s default)
            │  - Pure in-process evaluation, P99 < 1ms
            │  - Graceful degradation on server failure
            │
            │  REST: GET /api/v1/eval/sync?appId=xxx&since=xxx
            ▼
┌──────────────────────────────────────────────────────────┐
│  Feature Flag Server (SpringBoot, port 8080)              │
│                                                          │
│  Management API  (/api/v1/admin/*)                       │
│    CRUD flags, targeting rules, rollouts, applications    │
│                                                          │
│  Sync API  (/api/v1/eval/sync)                           │
│    Returns flag configs for a given application           │
│    - Initial: all flags bound to app (typically < 200)    │
│    - Incremental: only changed flags since timestamp      │
│                                                          │
│  Server-side cache: Redis (flags) + Caffeine L1           │
│  Flag change → DB write → Redis pub/sub → clear L1 cache  │
└───────────┬────────────────────┬─────────────────────────┘
            │                    │
    ┌───────▼──────┐    ┌───────▼──────┐
    │  MySQL  │    │    Redis     │
    │  (flag defs, │    │  (hot cache, │
    │   rules,     │    │   pub/sub)   │
    │   audit log) │    │              │
    └──────────────┘    └──────────────┘
```

## Project Structure

```
feature-flag-platform/
├── server/                          # SpringBoot service
│   ├── src/main/java/com/ffs/server/
│   │   ├── controller/
│   │   │   ├── SyncController           # GET /api/v1/eval/sync
│   │   │   ├── ManagementController     # CRUD /api/v1/admin/*
│   │   │   └── HealthController         # /api/v1/health
│   │   ├── service/
│   │   │   ├── FlagService              # Flag CRUD + targeting logic
│   │   │   ├── SyncService              # Build sync response
│   │   │   ├── ApplicationService       # App registration
│   │   │   └── AuditService             # Audit logging
│   │   ├── model/
│   │   │   ├── entity/                  # JPA entities: Flag, TargetingRule, Application
│   │   │   └── dto/                     # Request/response DTOs
│   │   ├── repository/                  # Spring Data JPA repositories
│   │   ├── cache/                       # Redis + Caffeine cache config
│   │   └── config/                      # App config, ObjectMapper, etc.
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/                # Flyway migrations
│   ├── src/test/java/                   # Integration + unit tests
│   ├── pom.xml
│   └── Dockerfile
│
├── sdk/                               # Java SDK library
│   ├── src/main/java/com/ffs/sdk/
│   │   ├── FeatureFlagClient           # Main entry point (builder pattern)
│   │   ├── FeatureFlagClientBuilder
│   │   ├── config/
│   │   │   └── ClientConfig
│   │   ├── model/
│   │   │   ├── FlagConfig              # Parsed flag definition
│   │   │   ├── TargetingRule           # Rule model
│   │   │   ├── EvalResult              # Result + trace (explainability)
│   │   │   └── FFUser                  # User context for evaluation
│   │   ├── evaluator/
│   │   │   └── RuleEvaluator           # Rule matching + rollout hash logic
│   │   ├── cache/
│   │   │   └── FlagCache               # Local in-memory flag store
│   │   └── sync/
│   │       └── ConfigPoller            # Periodic HTTP sync to server
│   ├── src/test/java/
│   │   └── RuleEvaluatorTest           # Unit tests with shared test cases
│   ├── pom.xml
│   └── README.md
│
├── sdk-spec/                          # Cross-platform SDK spec
│   ├── sdk-interface.md               # Interface contract for any SDK implementation
│   └── test-cases.json                # Shared test cases (language-agnostic)
│
├── sample/                            # Demo app using the SDK
│   ├── src/main/java/com/ffs/sample/
│   │   ├── SampleApplication          # SpringBoot app entry
│   │   └── controller/
│   │       └── DemoController         # Demonstrates isEnabled() usage
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── docker-compose.yml                 # mysql + redis + server + sample
├── init-db.sql                        # Seed data: sample flags + rules
└── README.md
```

## Data Model

### MySQL Tables

```sql
-- Application registration
CREATE TABLE application (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- Feature flag definition
CREATE TABLE flag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    key             VARCHAR(200) NOT NULL UNIQUE,
    name            VARCHAR(500),
    description     TEXT,
    flag_type       VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',  -- BOOLEAN, STRING, NUMBER
    default_value   TEXT NOT NULL DEFAULT 'false',
    enabled         BOOLEAN DEFAULT FALSE,                    -- master kill switch
    release_version VARCHAR(100),                             -- associated release
    created_by      VARCHAR(200),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Many-to-many: which apps see which flags
CREATE TABLE flag_application (
    flag_id        BIGINT REFERENCES flag(id),
    application_id BIGINT REFERENCES application(id),
    PRIMARY KEY (flag_id, application_id)
);

-- Targeting rule (evaluated by priority, first match wins)
CREATE TABLE targeting_rule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id     BIGINT NOT NULL REFERENCES flag(id) ON DELETE CASCADE,
    priority    INT NOT NULL,
    attribute   VARCHAR(100) NOT NULL,  -- e.g. region, plan, user_id
    operator    VARCHAR(50) NOT NULL,   -- EQUALS, IN, CONTAINS, GREATER_THAN, LESS_THAN
    value       TEXT NOT NULL,          -- JSON: "eu-west" or ["a@b.com","c@d.com"]
    serve_value TEXT NOT NULL,          -- Value to return when this rule matches
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Percentage rollout
CREATE TABLE rollout (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id     BIGINT NOT NULL REFERENCES flag(id) ON DELETE CASCADE,
    percentage  INT NOT NULL CHECK (percentage BETWEEN 0 AND 100),
    serve_value TEXT NOT NULL,
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Audit log for change tracking (explainability)
CREATE TABLE audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id     BIGINT REFERENCES flag(id),
    action      VARCHAR(50) NOT NULL,   -- CREATED, UPDATED, DELETED, RULE_ADDED, etc.
    changed_by  VARCHAR(200),
    detail      JSON,                  -- Full snapshot of what changed
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### Targeting Dimensions

| Dimension | Attribute Examples | Operator | Description |
|---|---|---|---|
| User targeting | `user_id`, `email`, `plan` | EQUALS, IN | Whitelist specific users |
| Geography | `region`, `country` | EQUALS, IN | Gate by location |
| Percentage rollout | — (hash-based) | HASH_RANGE | 0-100% gradual rollout |
| Application | `app_id` | EQUALS | Scope flag to specific apps |
| User tags | `tags` | CONTAINS | e.g. "internal", "beta" |

**Evaluation order:** Targeting rules (by priority) → Rollout → Default value. First match wins.

### Extensible Targeting (Not Hardcoded)

Targeting is **fully data-driven** — rules are stored in DB, not hardcoded in code. An admin UI or API call can create rules with any attribute:

```
Admin creates rule via API/UI:
  POST /api/v1/admin/flags/{id}/rules
  { "attribute": "membership_level", "operator": "EQUALS", "value": "vip", "serveValue": "true" }

→ Rule stored in DB
→ SDK syncs and evaluates it generically
→ Zero code changes required
```

The SDK's `FFUser` carries arbitrary attributes via a map:

```java
FFUser user = FFUser.builder()
    .id("user_123")
    .custom("membership_level", "vip")     // any attribute from admin UI
    .custom("device_type", "mobile")       // any attribute from admin UI
    .build();
```

Rule evaluation is generic — the evaluator checks `user.attributes.get(rule.attribute)` and applies `rule.operator`. Adding a new targeting dimension means adding a DB row, not changing code.

The table above lists **common** dimensions (shipped out of the box), not an exhaustive enumeration.

## API Design

### Sync API (used by SDK)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/eval/sync?appId={appId}` | Initial sync: return all flags for this app |
| GET | `/api/v1/eval/sync?appId={appId}&since={timestamp}` | Incremental sync: return flags changed since timestamp |

**Response:**
```json
{
  "flags": [
    {
      "key": "new_checkout_ui",
      "type": "BOOLEAN",
      "defaultValue": "false",
      "enabled": true,
      "releaseVersion": "v3.2.0",
      "updatedAt": "2026-07-03T06:30:00Z",
      "rules": [
        {
          "priority": 1,
          "attribute": "region",
          "operator": "EQUALS",
          "value": "eu-west",
          "serveValue": "true"
        }
      ],
      "rollout": {
        "percentage": 20,
        "serveValue": "true"
      }
    }
  ],
  "serverTime": "2026-07-05T10:00:00Z"
}
```

### Management API

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/admin/flags` | Create a flag |
| GET | `/api/v1/admin/flags` | List all flags (paginated) |
| GET | `/api/v1/admin/flags/{id}` | Get flag detail with rules |
| PUT | `/api/v1/admin/flags/{id}` | Update flag |
| DELETE | `/api/v1/admin/flags/{id}` | Delete flag |
| POST | `/api/v1/admin/flags/{id}/rules` | Add targeting rule |
| PUT | `/api/v1/admin/flags/{id}/rules/{ruleId}` | Update rule |
| DELETE | `/api/v1/admin/flags/{id}/rules/{ruleId}` | Delete rule |
| POST | `/api/v1/admin/flags/{id}/rollout` | Set rollout percentage |
| POST | `/api/v1/admin/flags/{id}/bind` | Bind flag to application |
| POST | `/api/v1/admin/applications` | Register application |
| GET | `/api/v1/admin/applications` | List applications |
| GET | `/api/v1/admin/flags/{id}/audit` | Get audit log for flag |
| GET | `/api/v1/health` | Health check |

## Caching Strategy

```
SDK Local (ConcurrentHashMap)
  TTL: driven by sync interval (10s)
  Strategy: store ALL flags for this app locally
  Hit: P99 < 1ms (pure Java object traversal)
  Miss: never (sync preloads everything)

Server L1 (Caffeine, per-node)
  TTL: 10s, max 1000 entries
  Purpose: absorb hot flags, avoid Redis round-trip

Server L2 (Redis)
  TTL: 60s
  Key pattern: flag:{flagId}, app-flags:{appId}
  Stores serialized flag config JSON

Database (MySQL)
  Source of truth
  Read-through: cache miss → load from DB → populate Redis + Caffeine

Cache Invalidation on Flag Change:
  Admin updates flag → write to DB → publish Redis event (flag:updated:{id})
  → Server nodes subscribe → evict Caffeine entry
  → SDK picks up next sync cycle (max 10s staleness)
```

## SDK Design

### Core Interface

```java
// Builder pattern for configuration
FeatureFlagClient client = FeatureFlagClient.builder()
    .serverUrl("http://feature-flag-server:8080")
    .appId("order-service")
    .syncInterval(Duration.ofSeconds(10))
    .build();

// Start the client (triggers initial sync + starts poller)
client.start();

// Evaluation — pure local, no network call
boolean enabled = client.isEnabled("new_checkout", user);
String  value   = client.stringValue("theme_color", user, "light");
Integer num     = client.intValue("max_items", user, 100);

// Batch evaluation (one traversal, multiple flags)
Map<String, EvalResult> results = client.evaluateAll(user, "flag_a", "flag_b");

// Graceful shutdown
client.close();
```

### EvalResult (Explainability)

```java
public class EvalResult {
    private String  flagKey;
    private String  value;           // The evaluated value
    private String  reason;          // "rule_match" | "rollout" | "default"
    private String  matchedRuleId;   // Which rule matched (null if default)
    private List<TraceEntry> trace;  // Step-by-step evaluation trace
    private String  releaseVersion;
    private Instant flagUpdatedAt;
}
```

### Behavior Guarantees

| Scenario | Behavior |
|---|---|
| Server unreachable at startup | Retry with backoff, use empty state (all defaults) |
| Server unreachable during operation | Continue using last-known-good cache |
| Flag not found locally | Return default value from the call argument |
| Sync returns error | Log warning, retry next cycle, keep current cache |
| `close()` called | Stop poller, release resources |

## Cross-platform SDK Consistency

Defined in `sdk-spec/`:

1. **sdk-interface.md** — documents the canonical interface all SDKs must implement
2. **test-cases.json** — shared test scenarios. Any SDK (Java, JS, Kotlin, etc.) runs these and must produce identical results

Example test case:
```json
{
  "name": "rule priority — first match wins",
  "flag": {
    "key": "test_flag",
    "type": "BOOLEAN",
    "defaultValue": "false",
    "rules": [
      { "priority": 1, "attribute": "region", "operator": "EQUALS", "value": "eu-west", "serveValue": "true" },
      { "priority": 2, "attribute": "plan",    "operator": "EQUALS", "value": "premium", "serveValue": "false" }
    ]
  },
  "user": { "id": "u1", "region": "eu-west", "plan": "premium" },
  "expected": "true"
}
```

## Observability

### Server Metrics (via Micrometer / Prometheus)

| Metric | Type | Description |
|---|---|---|
| `ff_sync_requests_total` | Counter | Total sync requests |
| `ff_sync_duration_ms` | Histogram | Sync request latency |
| `ff_admin_operations_total` | Counter | Admin CRUD operations by type |
| `ff_flags_total` | Gauge | Total number of flags |
| `ff_cache_hit_ratio` | Gauge | Server-side cache hit rate |

### SDK Metrics

| Metric | Type | Description |
|---|---|---|
| `ff_sdk_sync_success_total` | Counter | Successful syncs |
| `ff_sdk_sync_failure_total` | Counter | Failed syncs |
| `ff_sdk_evaluation_duration_ns` | Histogram | Local eval latency |
| `ff_sdk_cache_age_seconds` | Gauge | Age of local cache since last sync |

### Logging

- Structured JSON logs (for ELK/Loki ingestion)
- Every flag evaluation in sample app logs the full `EvalResult` (trace + reason)
- Server logs all admin mutations with `changed_by`

### Health Check

- `GET /api/v1/health` returns DB + Redis connectivity status

## Docker Compose

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: featureflags
      MYSQL_USER: ffuser
      MYSQL_PASSWORD: ffpass
    ports: ["3306:3306"]
    volumes: ["./init-db.sql:/docker-entrypoint-initdb.d/init.sql"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  server:
    build: ./server
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/featureflags
      SPRING_DATASOURCE_USERNAME: ffuser
      SPRING_DATASOURCE_PASSWORD: ffpass
      SPRING_REDIS_HOST: redis
    depends_on: [mysql, redis]

  sample:
    build: ./sample
    ports: ["8081:8081"]
    environment:
      FF_SERVER_URL: http://server:8080
      FF_APP_ID: sample-app
    depends_on: [server]
```

## Testing Strategy

### Server Tests
- **Unit:** FlagService, SyncService, Rule evaluation logic
- **Integration:** Full API tests with Testcontainers (MySQL + Redis)
- **Contract:** Verify sync API response schema matches SDK expectations

### SDK Tests
- **Unit:** RuleEvaluator (runs shared `test-cases.json`), FlagCache, ConfigPoller
- **Integration:** SDK against a running server (WireMock or Testcontainers)
- **Behavior:** Server-down resilience, cache staleness, default fallback

### Sample App
- Smoke test: `GET /demo/checkout?userId=xxx` returns different results based on flag state
