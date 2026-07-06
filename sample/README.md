# Sample App

Demo Spring Boot application that embeds the SDK to demonstrate feature flag evaluation in a real business scenario.

## How It Works

The SDK is a Java library — no HTTP server. You call Java methods directly, and evaluation happens in-process (P99 < 1ms) against a local `ConcurrentHashMap` cache.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/demo/checkout?userId=&region=&plan=` | Simulate checkout — evaluates 3 flags and returns results |
| `GET` | `/demo/explain/{flagKey}?userId=&region=&plan=` | Get explainability detail for a single flag |

## Example

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
    "new_checkout_ui": { "flagKey": "new_checkout_ui", "value": "true", "reason": "enabled", ... }
  }
}
```

## Startup Demo

`DemoRunner` runs on startup and logs evaluation results for 3 flags:

```bash
docker compose logs sample | grep -E "\[new_checkout_ui\]|\[dark_mode\]|\[max_search_results\]|Explain"
```

## Structure

```
DemoRunner.java       — Startup evaluation demo (logs)
DemoController.java   — HTTP endpoints for manual testing
FeatureFlagConfig.java — SDK client bean configuration
```
