# Feature Flag SDK

Java client library for feature flag evaluation. Evaluates flags in-process against a local cache (P99 < 1ms), synced from the server every 10 seconds.

## Architecture

```
FeatureFlagClient (entry point, builder pattern)
  ├── FlagCache (ConcurrentHashMap, thread-safe)
  ├── ConfigPoller (background thread, sync every 10s)
  └── RuleEvaluator (in-process evaluation, zero network)
```

## Quick Start

```java
// 1. Create and start the client (once at app startup)
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

// 3. Evaluate flags — pure local, P99 < 1ms
if (client.isEnabled("new_checkout_ui", user)) {
    renderNewUI();
}
String theme = client.stringValue("theme_color", user, "light");
int maxResults = client.intValue("max_search_results", user, 20);

// 4. Batch evaluation with explainability
Map<String, EvalResult> results = client.evaluateAll(user, "flag_a", "flag_b");
// EvalResult: value, reason, userId, region, releaseVersion, trace[]
```

## API

| Method | Returns | Description |
|--------|---------|-------------|
| `isEnabled(key, user)` | `boolean` | Evaluate a boolean flag |
| `stringValue(key, user, defaultVal)` | `String` | Evaluate a string flag |
| `intValue(key, user, defaultVal)` | `int` | Evaluate a number flag |
| `evaluateAll(user, ...keys)` | `Map<String, EvalResult>` | Batch evaluate with full explainability |

## Resilience

- **Startup retry:** exponential backoff (1s → 2s → 4s → 8s → 16s, max 5 attempts)
- **Runtime:** keeps using last-known-good cache if server is unreachable
- **Cold start:** returns user-provided default values when cache is empty

## Cache

- Local `ConcurrentHashMap` keyed by flag key
- Updated by ConfigPoller every sync interval (default 10s)
- `GET /api/v1/eval/sync?appId=X&since=<timestamp>` — incremental sync
- Picks up server changes within one sync interval

## Structure

```
FeatureFlagClient.java          Main entry point (builder)
FeatureFlagClientBuilder.java   Builder with fluent config
cache/FlagCache.java            Thread-safe ConcurrentHashMap wrapper
sync/ConfigPoller.java          Periodic sync with retry + backoff
evaluator/RuleEvaluator.java    Flag evaluation logic
model/                          FFUser, FlagConfig, EvalResult, TraceEntry
```
