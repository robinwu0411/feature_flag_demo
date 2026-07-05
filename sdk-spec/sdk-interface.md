# Feature Flag SDK Interface Specification v1.0

## Core API

| Method | Returns | Description |
|---|---|---|
| `isEnabled(flagKey, user)` | boolean | Evaluate boolean flag |
| `stringValue(flagKey, user, defaultValue)` | string | Evaluate string flag |
| `intValue(flagKey, user, defaultValue)` | integer | Evaluate numeric flag |
| `evaluateAll(user, ...flagKeys)` | Map<string,EvalResult> | Batch evaluate |

## FFUser

- `id` (string, required) — Unique user identifier
- Arbitrary key-value attributes for targeting

## EvalResult

- `flagKey`, `value`, `reason`, `matchedRuleId`, `trace`, `releaseVersion`, `flagUpdatedAt`

## Behavior

1. Evaluation MUST be local (no network calls)
2. Server unreachable → use last-known-good cache
3. Flag not in cache → use caller-provided default
4. Thread-safe for concurrent evaluation
