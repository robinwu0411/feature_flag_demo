# Feature Management Service

Feature flag platform for e-commerce with SpringBoot + MyBatis server and Java SDK.

## Quick Start

```bash
mvn clean package -DskipTests
docker-compose up -d
# Wait ~30s for MySQL init

# Test sample app
curl "http://localhost:8081/demo/checkout?userId=user_1&region=eu-west&plan=premium"

# Explainability trace
curl "http://localhost:8081/demo/explain/new_checkout_ui?userId=user_1&region=eu-west&plan=premium"

# Server health
curl http://localhost:8080/api/v1/health

# Management API
curl -X POST http://localhost:8080/api/v1/admin/flags \
  -H "Content-Type: application/json" \
  -d '{"key":"my_feature","name":"My Feature","flagType":"BOOLEAN","defaultValue":"false","enabled":true,"createdBy":"admin"}'
```
