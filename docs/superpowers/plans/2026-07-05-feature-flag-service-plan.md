# Feature Management Service — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a feature flag platform with a SpringBoot + MyBatis server, a Java SDK library, and a demo sample app — all orchestrated via Docker Compose.

**Architecture:** Monorepo with 3 Maven modules. Server manages flag configs (MySQL + MyBatis + Redis) and exposes REST APIs. SDK is a standalone JAR that syncs configs from server and evaluates flags in-process. Sample app depends on SDK and demonstrates usage.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis 3.0, MySQL 8.0, Redis 7, Maven multi-module, Docker Compose

---

### Task 1: Project Scaffolding

**Files:**
- Create: `pom.xml` (parent)
- Create: `server/pom.xml`
- Create: `sdk/pom.xml`
- Create: `sample/pom.xml`
- Create: `docker-compose.yml`
- Create: `init-db.sql`
- Create: `server/Dockerfile`
- Create: `sample/Dockerfile`
- Create: `server/src/main/resources/application.yml`
- Create: `server/src/main/java/com/ffs/server/FeatureFlagServerApplication.java`

- [ ] **Step 1: Create parent pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.ffs</groupId>
    <artifactId>feature-flag-platform</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <modules>
        <module>server</module>
        <module>sdk</module>
        <module>sample</module>
    </modules>
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
        <mybatis.version>3.0.3</mybatis.version>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: Create server/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ffs</groupId>
        <artifactId>feature-flag-platform</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>feature-flag-server</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create sdk/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ffs</groupId>
        <artifactId>feature-flag-platform</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>feature-flag-sdk-java</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create sample/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ffs</groupId>
        <artifactId>feature-flag-platform</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>feature-flag-sample</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ffs</groupId>
            <artifactId>feature-flag-sdk-java</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: Create docker-compose.yml**

```yaml
version: "3.8"
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: featureflags
      MYSQL_USER: ffuser
      MYSQL_PASSWORD: ffpass
    ports:
      - "3306:3306"
    volumes:
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  server:
    build: ./server
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/featureflags
      SPRING_DATASOURCE_USERNAME: ffuser
      SPRING_DATASOURCE_PASSWORD: ffpass
      SPRING_REDIS_HOST: redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  sample:
    build: ./sample
    ports:
      - "8081:8081"
    environment:
      FF_SERVER_URL: http://server:8080
      FF_APP_ID: sample-app
    depends_on:
      - server
```

- [ ] **Step 6: Create init-db.sql**

```sql
CREATE TABLE IF NOT EXISTS application (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `key`           VARCHAR(200) NOT NULL UNIQUE,
    name            VARCHAR(500),
    description     TEXT,
    flag_type       VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    default_value   TEXT NOT NULL DEFAULT 'false',
    enabled         BOOLEAN DEFAULT FALSE,
    release_version VARCHAR(100),
    created_by      VARCHAR(200),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flag_application (
    flag_id        BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    PRIMARY KEY (flag_id, application_id),
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS targeting_rule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id     BIGINT NOT NULL,
    priority    INT NOT NULL,
    attribute   VARCHAR(100) NOT NULL,
    operator    VARCHAR(50) NOT NULL,
    value       TEXT NOT NULL,
    serve_value TEXT NOT NULL,
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rollout (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id     BIGINT NOT NULL,
    percentage  INT NOT NULL,
    serve_value TEXT NOT NULL,
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id     BIGINT,
    action      VARCHAR(50) NOT NULL,
    changed_by  VARCHAR(200),
    detail      JSON,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE SET NULL
);

-- Seed data
INSERT INTO application (id, name, description) VALUES (1, 'sample-app', 'Demo application');

INSERT INTO flag (id, `key`, name, flag_type, default_value, enabled, release_version, created_by)
VALUES (1, 'new_checkout_ui', 'New Checkout UI', 'BOOLEAN', 'false', TRUE, 'v3.2.0', 'admin');

INSERT INTO flag (id, `key`, name, flag_type, default_value, enabled, release_version, created_by)
VALUES (2, 'dark_mode', 'Dark Mode Theme', 'BOOLEAN', 'false', TRUE, 'v3.1.0', 'admin');

INSERT INTO flag (id, `key`, name, flag_type, default_value, enabled, release_version, created_by)
VALUES (3, 'max_search_results', 'Max Search Results', 'NUMBER', '20', TRUE, 'v3.0.0', 'admin');

INSERT INTO flag_application (flag_id, application_id) VALUES (1, 1), (2, 1), (3, 1);

INSERT INTO targeting_rule (id, flag_id, priority, attribute, operator, value, serve_value, enabled)
VALUES (1, 1, 1, 'region', 'EQUALS', '"eu-west"', 'true', TRUE);

INSERT INTO targeting_rule (id, flag_id, priority, attribute, operator, value, serve_value, enabled)
VALUES (2, 1, 2, 'plan', 'EQUALS', '"premium"', 'true', TRUE);

INSERT INTO targeting_rule (id, flag_id, priority, attribute, operator, value, serve_value, enabled)
VALUES (3, 2, 1, 'region', 'EQUALS', '"eu-west"', 'true', TRUE);

INSERT INTO rollout (id, flag_id, percentage, serve_value, enabled)
VALUES (1, 2, 50, 'true', TRUE);
```

- [ ] **Step 7: Create server/Dockerfile**

```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 8: Create sample/Dockerfile**

```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 9: Create server/src/main/resources/application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/featureflags}
    username: ${SPRING_DATASOURCE_USERNAME:ffuser}
    password: ${SPRING_DATASOURCE_PASSWORD:ffpass}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: 6379
  cache:
    type: redis
    redis:
      time-to-live: 60000

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.ffs.server.model.entity
  configuration:
    map-underscore-to-camel-case: true

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

- [ ] **Step 10: Create main class**

`server/src/main/java/com/ffs/server/FeatureFlagServerApplication.java`:

```java
package com.ffs.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@MapperScan("com.ffs.server.mapper")
public class FeatureFlagServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeatureFlagServerApplication.class, args);
    }
}
```

- [ ] **Step 11: Verify scaffolding compiles**

Run: `mvn clean compile -pl server -am`
Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git init
git add pom.xml server/pom.xml sdk/pom.xml sample/pom.xml docker-compose.yml init-db.sql \
        server/Dockerfile sample/Dockerfile server/src/main/
git commit -m "feat: project scaffolding with Maven multi-module, MyBatis, Docker Compose"
```

---

### Task 2: Server — Entities (POJOs)

Plain Java objects — no JPA annotations. MyBatis handles mapping via XML.

**Files:**
- Create: `server/src/main/java/com/ffs/server/model/entity/Application.java`
- Create: `server/src/main/java/com/ffs/server/model/entity/Flag.java`
- Create: `server/src/main/java/com/ffs/server/model/entity/TargetingRule.java`
- Create: `server/src/main/java/com/ffs/server/model/entity/Rollout.java`
- Create: `server/src/main/java/com/ffs/server/model/entity/AuditLog.java`

- [ ] **Step 1: Create Application**

`server/src/main/java/com/ffs/server/model/entity/Application.java`:

```java
package com.ffs.server.model.entity;

import java.time.Instant;

public class Application {
    private Long id;
    private String name;
    private String description;
    private Boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    public Application() {}
    public Application(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create Flag**

`server/src/main/java/com/ffs/server/model/entity/Flag.java`:

```java
package com.ffs.server.model.entity;

import java.time.Instant;

public class Flag {
    private Long id;
    private String key;
    private String name;
    private String description;
    private String flagType = "BOOLEAN";
    private String defaultValue = "false";
    private Boolean enabled = false;
    private String releaseVersion;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Flag() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFlagType() { return flagType; }
    public void setFlagType(String flagType) { this.flagType = flagType; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create TargetingRule**

`server/src/main/java/com/ffs/server/model/entity/TargetingRule.java`:

```java
package com.ffs.server.model.entity;

import java.time.Instant;

public class TargetingRule {
    private Long id;
    private Long flagId;
    private Integer priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;
    private Boolean enabled = true;
    private Instant createdAt;

    public TargetingRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlagId() { return flagId; }
    public void setFlagId(Long flagId) { this.flagId = flagId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: Create Rollout**

`server/src/main/java/com/ffs/server/model/entity/Rollout.java`:

```java
package com.ffs.server.model.entity;

import java.time.Instant;

public class Rollout {
    private Long id;
    private Long flagId;
    private Integer percentage;
    private String serveValue;
    private Boolean enabled = true;
    private Instant createdAt;

    public Rollout() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlagId() { return flagId; }
    public void setFlagId(Long flagId) { this.flagId = flagId; }
    public Integer getPercentage() { return percentage; }
    public void setPercentage(Integer percentage) { this.percentage = percentage; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 5: Create AuditLog**

`server/src/main/java/com/ffs/server/model/entity/AuditLog.java`:

```java
package com.ffs.server.model.entity;

import java.time.Instant;

public class AuditLog {
    private Long id;
    private Long flagId;
    private String action;
    private String changedBy;
    private String detail;
    private Instant createdAt;

    public AuditLog() {}

    public AuditLog(Long flagId, String action, String changedBy, String detail) {
        this.flagId = flagId;
        this.action = action;
        this.changedBy = changedBy;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlagId() { return flagId; }
    public void setFlagId(Long flagId) { this.flagId = flagId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 6: Verify compilation**

Run: `mvn clean compile -pl server -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add server/src/main/java/com/ffs/server/model/entity/
git commit -m "feat: add entity POJOs for MyBatis"
```

---

### Task 3: Server — Mapper Interfaces + XML

**Files:**
- Create: `server/src/main/java/com/ffs/server/mapper/FlagMapper.java`
- Create: `server/src/main/java/com/ffs/server/mapper/TargetingRuleMapper.java`
- Create: `server/src/main/java/com/ffs/server/mapper/RolloutMapper.java`
- Create: `server/src/main/java/com/ffs/server/mapper/ApplicationMapper.java`
- Create: `server/src/main/java/com/ffs/server/mapper/AuditLogMapper.java`
- Create: `server/src/main/resources/mapper/FlagMapper.xml`
- Create: `server/src/main/resources/mapper/TargetingRuleMapper.xml`
- Create: `server/src/main/resources/mapper/RolloutMapper.xml`
- Create: `server/src/main/resources/mapper/ApplicationMapper.xml`
- Create: `server/src/main/resources/mapper/AuditLogMapper.xml`

- [ ] **Step 1: Create mapper interfaces**

`server/src/main/java/com/ffs/server/mapper/FlagMapper.java`:

```java
package com.ffs.server.mapper;

import com.ffs.server.model.entity.Flag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface FlagMapper {
    Flag findById(Long id);
    Flag findByKey(String key);
    List<Flag> findAll();
    List<Flag> findByApplicationId(@Param("appId") Long appId);
    List<Flag> findByApplicationIdAndUpdatedAfter(@Param("appId") Long appId, @Param("since") Instant since);
    int insert(Flag flag);
    int update(Flag flag);
    int deleteById(Long id);
    int insertFlagApplication(@Param("flagId") Long flagId, @Param("applicationId") Long appId);
    List<Long> findApplicationIdsByFlagId(@Param("flagId") Long flagId);
}
```

`server/src/main/java/com/ffs/server/mapper/TargetingRuleMapper.java`:

```java
package com.ffs.server.mapper;

import com.ffs.server.model.entity.TargetingRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TargetingRuleMapper {
    List<TargetingRule> findByFlagIdOrderByPriority(Long flagId);
    TargetingRule findById(Long id);
    int insert(TargetingRule rule);
    int update(TargetingRule rule);
    int deleteById(Long id);
    int deactivateByFlagId(Long flagId);
}
```

`server/src/main/java/com/ffs/server/mapper/RolloutMapper.java`:

```java
package com.ffs.server.mapper;

import com.ffs.server.model.entity.Rollout;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolloutMapper {
    List<Rollout> findByFlagId(Long flagId);
    int insert(Rollout rollout);
    int deactivateByFlagId(Long flagId);
}
```

`server/src/main/java/com/ffs/server/mapper/ApplicationMapper.java`:

```java
package com.ffs.server.mapper;

import com.ffs.server.model.entity.Application;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApplicationMapper {
    Application findById(Long id);
    Application findByName(String name);
    List<Application> findAll();
    int insert(Application app);
}
```

`server/src/main/java/com/ffs/server/mapper/AuditLogMapper.java`:

```java
package com.ffs.server.mapper;

import com.ffs.server.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AuditLogMapper {
    int insert(AuditLog log);
    List<AuditLog> findByFlagIdOrderByCreatedAtDesc(Long flagId);
}
```

- [ ] **Step 2: Create mapper XML files**

`server/src/main/resources/mapper/FlagMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ffs.server.mapper.FlagMapper">

    <resultMap id="flagMap" type="com.ffs.server.model.entity.Flag">
        <id property="id" column="id"/>
        <result property="key" column="`key`"/>
        <result property="name" column="name"/>
        <result property="description" column="description"/>
        <result property="flagType" column="flag_type"/>
        <result property="defaultValue" column="default_value"/>
        <result property="enabled" column="enabled"/>
        <result property="releaseVersion" column="release_version"/>
        <result property="createdBy" column="created_by"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="findById" resultMap="flagMap">
        SELECT * FROM flag WHERE id = #{id}
    </select>

    <select id="findByKey" resultMap="flagMap">
        SELECT * FROM flag WHERE `key` = #{key}
    </select>

    <select id="findAll" resultMap="flagMap">
        SELECT * FROM flag ORDER BY id
    </select>

    <select id="findByApplicationId" resultMap="flagMap">
        SELECT f.* FROM flag f
        INNER JOIN flag_application fa ON f.id = fa.flag_id
        WHERE fa.application_id = #{appId}
    </select>

    <select id="findByApplicationIdAndUpdatedAfter" resultMap="flagMap">
        SELECT f.* FROM flag f
        INNER JOIN flag_application fa ON f.id = fa.flag_id
        WHERE fa.application_id = #{appId} AND f.updated_at > #{since}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO flag (`key`, name, description, flag_type, default_value, enabled, release_version, created_by, created_at, updated_at)
        VALUES (#{key}, #{name}, #{description}, #{flagType}, #{defaultValue}, #{enabled}, #{releaseVersion}, #{createdBy}, NOW(), NOW())
    </insert>

    <update id="update">
        UPDATE flag SET
            name = #{name},
            description = #{description},
            default_value = #{defaultValue},
            enabled = #{enabled},
            release_version = #{releaseVersion},
            updated_at = NOW()
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM flag WHERE id = #{id}
    </delete>

    <insert id="insertFlagApplication">
        INSERT INTO flag_application (flag_id, application_id) VALUES (#{flagId}, #{applicationId})
    </insert>

    <select id="findApplicationIdsByFlagId" resultType="long">
        SELECT application_id FROM flag_application WHERE flag_id = #{flagId}
    </select>
</mapper>
```

`server/src/main/resources/mapper/TargetingRuleMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ffs.server.mapper.TargetingRuleMapper">

    <resultMap id="ruleMap" type="com.ffs.server.model.entity.TargetingRule">
        <id property="id" column="id"/>
        <result property="flagId" column="flag_id"/>
        <result property="priority" column="priority"/>
        <result property="attribute" column="attribute"/>
        <result property="operator" column="operator"/>
        <result property="value" column="value"/>
        <result property="serveValue" column="serve_value"/>
        <result property="enabled" column="enabled"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <select id="findByFlagIdOrderByPriority" resultMap="ruleMap">
        SELECT * FROM targeting_rule WHERE flag_id = #{flagId} AND enabled = TRUE ORDER BY priority ASC
    </select>

    <select id="findById" resultMap="ruleMap">
        SELECT * FROM targeting_rule WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO targeting_rule (flag_id, priority, attribute, operator, value, serve_value, enabled, created_at)
        VALUES (#{flagId}, #{priority}, #{attribute}, #{operator}, #{value}, #{serveValue}, #{enabled}, NOW())
    </insert>

    <update id="update">
        UPDATE targeting_rule SET
            priority = #{priority},
            attribute = #{attribute},
            operator = #{operator},
            value = #{value},
            serve_value = #{serveValue},
            enabled = #{enabled}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM targeting_rule WHERE id = #{id}
    </delete>

    <update id="deactivateByFlagId">
        UPDATE targeting_rule SET enabled = FALSE WHERE flag_id = #{flagId}
    </update>
</mapper>
```

`server/src/main/resources/mapper/RolloutMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ffs.server.mapper.RolloutMapper">

    <resultMap id="rolloutMap" type="com.ffs.server.model.entity.Rollout">
        <id property="id" column="id"/>
        <result property="flagId" column="flag_id"/>
        <result property="percentage" column="percentage"/>
        <result property="serveValue" column="serve_value"/>
        <result property="enabled" column="enabled"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <select id="findByFlagId" resultMap="rolloutMap">
        SELECT * FROM rollout WHERE flag_id = #{flagId} AND enabled = TRUE
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO rollout (flag_id, percentage, serve_value, enabled, created_at)
        VALUES (#{flagId}, #{percentage}, #{serveValue}, #{enabled}, NOW())
    </insert>

    <update id="deactivateByFlagId">
        UPDATE rollout SET enabled = FALSE WHERE flag_id = #{flagId}
    </update>
</mapper>
```

`server/src/main/resources/mapper/ApplicationMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ffs.server.mapper.ApplicationMapper">

    <resultMap id="appMap" type="com.ffs.server.model.entity.Application">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="description" column="description"/>
        <result property="enabled" column="enabled"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="findById" resultMap="appMap">
        SELECT * FROM application WHERE id = #{id}
    </select>

    <select id="findByName" resultMap="appMap">
        SELECT * FROM application WHERE name = #{name}
    </select>

    <select id="findAll" resultMap="appMap">
        SELECT * FROM application ORDER BY id
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO application (name, description, enabled, created_at, updated_at)
        VALUES (#{name}, #{description}, #{enabled}, NOW(), NOW())
    </insert>
</mapper>
```

`server/src/main/resources/mapper/AuditLogMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ffs.server.mapper.AuditLogMapper">

    <resultMap id="auditMap" type="com.ffs.server.model.entity.AuditLog">
        <id property="id" column="id"/>
        <result property="flagId" column="flag_id"/>
        <result property="action" column="action"/>
        <result property="changedBy" column="changed_by"/>
        <result property="detail" column="detail"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO audit_log (flag_id, action, changed_by, detail, created_at)
        VALUES (#{flagId}, #{action}, #{changedBy}, #{detail}, NOW())
    </insert>

    <select id="findByFlagIdOrderByCreatedAtDesc" resultMap="auditMap">
        SELECT * FROM audit_log WHERE flag_id = #{flagId} ORDER BY created_at DESC
    </select>
</mapper>
```

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -pl server -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/src/main/java/com/ffs/server/mapper/ server/src/main/resources/mapper/
git commit -m "feat: add MyBatis mapper interfaces and XML"
```

---

### Task 4: Server — DTOs

**Files:**
- Create: `server/src/main/java/com/ffs/server/model/dto/SyncResponse.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/FlagDto.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/RuleDto.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/RolloutDto.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/CreateFlagRequest.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/CreateRuleRequest.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/CreateRolloutRequest.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/CreateApplicationRequest.java`
- Create: `server/src/main/java/com/ffs/server/model/dto/BindFlagRequest.java`

- [ ] **Step 1: Create all DTOs**

`server/src/main/java/com/ffs/server/model/dto/RuleDto.java`:

```java
package com.ffs.server.model.dto;

import com.ffs.server.model.entity.TargetingRule;

public class RuleDto {
    private Long id;
    private Integer priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;

    public static RuleDto from(TargetingRule r) {
        RuleDto d = new RuleDto();
        d.id = r.getId();
        d.priority = r.getPriority();
        d.attribute = r.getAttribute();
        d.operator = r.getOperator();
        d.value = r.getValue();
        d.serveValue = r.getServeValue();
        return d;
    }

    public Long getId() { return id; }
    public Integer getPriority() { return priority; }
    public String getAttribute() { return attribute; }
    public String getOperator() { return operator; }
    public String getValue() { return value; }
    public String getServeValue() { return serveValue; }
}
```

`server/src/main/java/com/ffs/server/model/dto/RolloutDto.java`:

```java
package com.ffs.server.model.dto;

import com.ffs.server.model.entity.Rollout;

public class RolloutDto {
    private Integer percentage;
    private String serveValue;

    public static RolloutDto from(Rollout r) {
        RolloutDto d = new RolloutDto();
        d.percentage = r.getPercentage();
        d.serveValue = r.getServeValue();
        return d;
    }

    public Integer getPercentage() { return percentage; }
    public String getServeValue() { return serveValue; }
}
```

`server/src/main/java/com/ffs/server/model/dto/FlagDto.java`:

```java
package com.ffs.server.model.dto;

import com.ffs.server.model.entity.Flag;
import com.ffs.server.model.entity.Rollout;
import com.ffs.server.model.entity.TargetingRule;

import java.time.Instant;
import java.util.List;

public class FlagDto {
    private String key;
    private String type;
    private String defaultValue;
    private Boolean enabled;
    private String releaseVersion;
    private Instant updatedAt;
    private List<RuleDto> rules;
    private RolloutDto rollout;

    public static FlagDto from(Flag flag, List<TargetingRule> rules, List<Rollout> rollouts) {
        FlagDto d = new FlagDto();
        d.key = flag.getKey();
        d.type = flag.getFlagType();
        d.defaultValue = flag.getDefaultValue();
        d.enabled = flag.getEnabled();
        d.releaseVersion = flag.getReleaseVersion();
        d.updatedAt = flag.getUpdatedAt();
        d.rules = rules.stream().map(RuleDto::from).toList();
        if (!rollouts.isEmpty()) {
            d.rollout = RolloutDto.from(rollouts.get(0));
        }
        return d;
    }

    public String getKey() { return key; }
    public String getType() { return type; }
    public String getDefaultValue() { return defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RuleDto> getRules() { return rules; }
    public RolloutDto getRollout() { return rollout; }
}
```

`server/src/main/java/com/ffs/server/model/dto/SyncResponse.java`:

```java
package com.ffs.server.model.dto;

import java.time.Instant;
import java.util.List;

public class SyncResponse {
    private List<FlagDto> flags;
    private Instant serverTime;

    public SyncResponse(List<FlagDto> flags) {
        this.flags = flags;
        this.serverTime = Instant.now();
    }

    public List<FlagDto> getFlags() { return flags; }
    public Instant getServerTime() { return serverTime; }
}
```

`server/src/main/java/com/ffs/server/model/dto/CreateFlagRequest.java`:

```java
package com.ffs.server.model.dto;

public class CreateFlagRequest {
    private String key;
    private String name;
    private String description;
    private String flagType = "BOOLEAN";
    private String defaultValue = "false";
    private Boolean enabled = false;
    private String releaseVersion;
    private String createdBy;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFlagType() { return flagType; }
    public void setFlagType(String flagType) { this.flagType = flagType; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
```

`server/src/main/java/com/ffs/server/model/dto/CreateRuleRequest.java`:

```java
package com.ffs.server.model.dto;

public class CreateRuleRequest {
    private Integer priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;
    private Boolean enabled = true;

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
```

`server/src/main/java/com/ffs/server/model/dto/CreateRolloutRequest.java`:

```java
package com.ffs.server.model.dto;

public class CreateRolloutRequest {
    private Integer percentage;
    private String serveValue;
    private Boolean enabled = true;

    public Integer getPercentage() { return percentage; }
    public void setPercentage(Integer percentage) { this.percentage = percentage; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
```

`server/src/main/java/com/ffs/server/model/dto/CreateApplicationRequest.java`:

```java
package com.ffs.server.model.dto;

public class CreateApplicationRequest {
    private String name;
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

`server/src/main/java/com/ffs/server/model/dto/BindFlagRequest.java`:

```java
package com.ffs.server.model.dto;

public class BindFlagRequest {
    private Long applicationId;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -pl server -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/src/main/java/com/ffs/server/model/dto/
git commit -m "feat: add server DTOs"
```

---

### Task 5: Server — Services

**Files:**
- Create: `server/src/main/java/com/ffs/server/service/AuditService.java`
- Create: `server/src/main/java/com/ffs/server/service/ApplicationService.java`
- Create: `server/src/main/java/com/ffs/server/service/FlagService.java`
- Create: `server/src/main/java/com/ffs/server/service/SyncService.java`

- [ ] **Step 1: Create AuditService**

`server/src/main/java/com/ffs/server/service/AuditService.java`:

```java
package com.ffs.server.service;

import com.ffs.server.mapper.AuditLogMapper;
import com.ffs.server.model.entity.AuditLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void log(Long flagId, String action, String changedBy, String detail) {
        auditLogMapper.insert(new AuditLog(flagId, action, changedBy, detail));
    }

    public List<AuditLog> getAuditLogs(Long flagId) {
        return auditLogMapper.findByFlagIdOrderByCreatedAtDesc(flagId);
    }
}
```

- [ ] **Step 2: Create ApplicationService**

`server/src/main/java/com/ffs/server/service/ApplicationService.java`:

```java
package com.ffs.server.service;

import com.ffs.server.mapper.ApplicationMapper;
import com.ffs.server.model.entity.Application;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationMapper applicationMapper;

    public ApplicationService(ApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    public Application create(String name, String description) {
        Application app = new Application(name, description);
        applicationMapper.insert(app);
        return app;
    }

    public List<Application> listAll() {
        return applicationMapper.findAll();
    }

    public Application getById(Long id) {
        Application app = applicationMapper.findById(id);
        if (app == null) throw new RuntimeException("Application not found: " + id);
        return app;
    }

    public Application getByName(String name) {
        Application app = applicationMapper.findByName(name);
        if (app == null) throw new RuntimeException("Application not found: " + name);
        return app;
    }
}
```

- [ ] **Step 3: Create FlagService**

`server/src/main/java/com/ffs/server/service/FlagService.java`:

```java
package com.ffs.server.service;

import com.ffs.server.mapper.*;
import com.ffs.server.model.dto.*;
import com.ffs.server.model.entity.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlagService {
    private final FlagMapper flagMapper;
    private final TargetingRuleMapper ruleMapper;
    private final RolloutMapper rolloutMapper;
    private final ApplicationService applicationService;
    private final AuditService auditService;

    public FlagService(FlagMapper flagMapper,
                       TargetingRuleMapper ruleMapper,
                       RolloutMapper rolloutMapper,
                       ApplicationService applicationService,
                       AuditService auditService) {
        this.flagMapper = flagMapper;
        this.ruleMapper = ruleMapper;
        this.rolloutMapper = rolloutMapper;
        this.applicationService = applicationService;
        this.auditService = auditService;
    }

    @CacheEvict(value = "flags", key = "#request.key")
    public Flag create(CreateFlagRequest request) {
        Flag flag = new Flag();
        flag.setKey(request.getKey());
        flag.setName(request.getName());
        flag.setDescription(request.getDescription());
        flag.setFlagType(request.getFlagType());
        flag.setDefaultValue(request.getDefaultValue());
        flag.setEnabled(request.getEnabled() != null ? request.getEnabled() : false);
        flag.setReleaseVersion(request.getReleaseVersion());
        flag.setCreatedBy(request.getCreatedBy());
        flagMapper.insert(flag);
        auditService.log(flag.getId(), "CREATED", request.getCreatedBy(),
                "{\"key\":\"" + request.getKey() + "\"}");
        return flag;
    }

    public List<Flag> listAll() {
        return flagMapper.findAll();
    }

    public Flag getById(Long id) {
        Flag flag = flagMapper.findById(id);
        if (flag == null) throw new RuntimeException("Flag not found: " + id);
        return flag;
    }

    public Flag getByKey(String key) {
        Flag flag = flagMapper.findByKey(key);
        if (flag == null) throw new RuntimeException("Flag not found: " + key);
        return flag;
    }

    @CacheEvict(value = "flags", key = "#result.key")
    public Flag update(Long id, CreateFlagRequest request) {
        Flag flag = getById(id);
        if (request.getName() != null) flag.setName(request.getName());
        if (request.getDescription() != null) flag.setDescription(request.getDescription());
        if (request.getDefaultValue() != null) flag.setDefaultValue(request.getDefaultValue());
        if (request.getEnabled() != null) flag.setEnabled(request.getEnabled());
        if (request.getReleaseVersion() != null) flag.setReleaseVersion(request.getReleaseVersion());
        flagMapper.update(flag);
        auditService.log(flag.getId(), "UPDATED", request.getCreatedBy(), "{}");
        return getById(id);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public void delete(Long id) {
        Flag flag = getById(id);
        flagMapper.deleteById(id);
        auditService.log(id, "DELETED", "system", "{\"key\":\"" + flag.getKey() + "\"}");
    }

    @CacheEvict(value = "flags", allEntries = true)
    public TargetingRule addRule(Long flagId, CreateRuleRequest request) {
        TargetingRule rule = new TargetingRule();
        rule.setFlagId(flagId);
        rule.setPriority(request.getPriority());
        rule.setAttribute(request.getAttribute());
        rule.setOperator(request.getOperator());
        rule.setValue(request.getValue());
        rule.setServeValue(request.getServeValue());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        ruleMapper.insert(rule);
        auditService.log(flagId, "RULE_ADDED", "admin",
                "{\"attribute\":\"" + request.getAttribute() + "\"}");
        return rule;
    }

    public List<TargetingRule> getRules(Long flagId) {
        return ruleMapper.findByFlagIdOrderByPriority(flagId);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public TargetingRule updateRule(Long flagId, Long ruleId, CreateRuleRequest request) {
        TargetingRule rule = ruleMapper.findById(ruleId);
        if (rule == null) throw new RuntimeException("Rule not found: " + ruleId);
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getAttribute() != null) rule.setAttribute(request.getAttribute());
        if (request.getOperator() != null) rule.setOperator(request.getOperator());
        if (request.getValue() != null) rule.setValue(request.getValue());
        if (request.getServeValue() != null) rule.setServeValue(request.getServeValue());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());
        ruleMapper.update(rule);
        return rule;
    }

    @CacheEvict(value = "flags", allEntries = true)
    public void deleteRule(Long flagId, Long ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public Rollout setRollout(Long flagId, CreateRolloutRequest request) {
        rolloutMapper.deactivateByFlagId(flagId);
        Rollout rollout = new Rollout();
        rollout.setFlagId(flagId);
        rollout.setPercentage(request.getPercentage());
        rollout.setServeValue(request.getServeValue());
        rollout.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        rolloutMapper.insert(rollout);
        auditService.log(flagId, "ROLLOUT_SET", "admin",
                "{\"percentage\":" + request.getPercentage() + "}");
        return rollout;
    }

    public List<Rollout> getRollouts(Long flagId) {
        return rolloutMapper.findByFlagId(flagId);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public void bindApplication(Long flagId, Long applicationId) {
        flagMapper.insertFlagApplication(flagId, applicationId);
    }
}
```

- [ ] **Step 4: Create SyncService**

`server/src/main/java/com/ffs/server/service/SyncService.java`:

```java
package com.ffs.server.service;

import com.ffs.server.mapper.FlagMapper;
import com.ffs.server.mapper.RolloutMapper;
import com.ffs.server.mapper.TargetingRuleMapper;
import com.ffs.server.model.dto.FlagDto;
import com.ffs.server.model.dto.SyncResponse;
import com.ffs.server.model.entity.Application;
import com.ffs.server.model.entity.Flag;
import com.ffs.server.model.entity.Rollout;
import com.ffs.server.model.entity.TargetingRule;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SyncService {
    private final FlagMapper flagMapper;
    private final TargetingRuleMapper ruleMapper;
    private final RolloutMapper rolloutMapper;
    private final ApplicationService applicationService;

    public SyncService(FlagMapper flagMapper,
                       TargetingRuleMapper ruleMapper,
                       RolloutMapper rolloutMapper,
                       ApplicationService applicationService) {
        this.flagMapper = flagMapper;
        this.ruleMapper = ruleMapper;
        this.rolloutMapper = rolloutMapper;
        this.applicationService = applicationService;
    }

    public SyncResponse sync(String appName, String sinceParam) {
        Application app = applicationService.getByName(appName);
        Long appId = app.getId();

        List<Flag> flags;
        if (sinceParam != null && !sinceParam.isEmpty()) {
            Instant since = Instant.parse(sinceParam);
            flags = flagMapper.findByApplicationIdAndUpdatedAfter(appId, since);
        } else {
            flags = flagMapper.findByApplicationId(appId);
        }

        List<FlagDto> dtos = flags.stream()
                .filter(Flag::getEnabled)
                .map(flag -> {
                    List<TargetingRule> rules = ruleMapper.findByFlagIdOrderByPriority(flag.getId());
                    List<Rollout> rollouts = rolloutMapper.findByFlagId(flag.getId());
                    return FlagDto.from(flag, rules, rollouts);
                })
                .toList();

        return new SyncResponse(dtos);
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -pl server -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/ffs/server/service/
git commit -m "feat: add server services using MyBatis mappers"
```

---

### Task 6: Server — Controllers

**Files:**
- Create: `server/src/main/java/com/ffs/server/controller/SyncController.java`
- Create: `server/src/main/java/com/ffs/server/controller/ManagementController.java`
- Create: `server/src/main/java/com/ffs/server/controller/HealthController.java`

- [ ] **Step 1: Create SyncController**

`server/src/main/java/com/ffs/server/controller/SyncController.java`:

```java
package com.ffs.server.controller;

import com.ffs.server.model.dto.SyncResponse;
import com.ffs.server.service.SyncService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/eval")
public class SyncController {
    private final SyncService syncService;
    private final Counter syncRequestCounter;
    private final Timer syncTimer;

    public SyncController(SyncService syncService, MeterRegistry registry) {
        this.syncService = syncService;
        this.syncRequestCounter = Counter.builder("ff_sync_requests_total").register(registry);
        this.syncTimer = Timer.builder("ff_sync_duration_ms").register(registry);
    }

    @GetMapping("/sync")
    public ResponseEntity<SyncResponse> sync(
            @RequestParam("appId") String appId,
            @RequestParam(value = "since", required = false) String since) {
        syncRequestCounter.increment();
        long start = System.currentTimeMillis();
        try {
            return ResponseEntity.ok(syncService.sync(appId, since));
        } finally {
            syncTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        }
    }
}
```

- [ ] **Step 2: Create ManagementController**

`server/src/main/java/com/ffs/server/controller/ManagementController.java`:

```java
package com.ffs.server.controller;

import com.ffs.server.model.dto.*;
import com.ffs.server.model.entity.*;
import com.ffs.server.service.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class ManagementController {
    private final FlagService flagService;
    private final ApplicationService applicationService;
    private final AuditService auditService;
    private final Counter adminOpsCounter;

    public ManagementController(FlagService flagService,
                                ApplicationService applicationService,
                                AuditService auditService,
                                MeterRegistry registry) {
        this.flagService = flagService;
        this.applicationService = applicationService;
        this.auditService = auditService;
        this.adminOpsCounter = Counter.builder("ff_admin_operations_total").register(registry);
    }

    @PostMapping("/flags")
    public ResponseEntity<Flag> createFlag(@RequestBody CreateFlagRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.status(HttpStatus.CREATED).body(flagService.create(request));
    }

    @GetMapping("/flags")
    public ResponseEntity<List<Flag>> listFlags() {
        return ResponseEntity.ok(flagService.listAll());
    }

    @GetMapping("/flags/{id}")
    public ResponseEntity<Flag> getFlag(@PathVariable Long id) {
        return ResponseEntity.ok(flagService.getById(id));
    }

    @PutMapping("/flags/{id}")
    public ResponseEntity<Flag> updateFlag(@PathVariable Long id, @RequestBody CreateFlagRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.ok(flagService.update(id, request));
    }

    @DeleteMapping("/flags/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable Long id) {
        adminOpsCounter.increment();
        flagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flags/{id}/rules")
    public ResponseEntity<TargetingRule> addRule(@PathVariable Long id, @RequestBody CreateRuleRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.status(HttpStatus.CREATED).body(flagService.addRule(id, request));
    }

    @GetMapping("/flags/{id}/rules")
    public ResponseEntity<List<TargetingRule>> getRules(@PathVariable Long id) {
        return ResponseEntity.ok(flagService.getRules(id));
    }

    @PutMapping("/flags/{id}/rules/{ruleId}")
    public ResponseEntity<TargetingRule> updateRule(@PathVariable Long id, @PathVariable Long ruleId,
                                                     @RequestBody CreateRuleRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.ok(flagService.updateRule(id, ruleId, request));
    }

    @DeleteMapping("/flags/{id}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id, @PathVariable Long ruleId) {
        adminOpsCounter.increment();
        flagService.deleteRule(id, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flags/{id}/rollout")
    public ResponseEntity<Rollout> setRollout(@PathVariable Long id, @RequestBody CreateRolloutRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.ok(flagService.setRollout(id, request));
    }

    @PostMapping("/flags/{id}/bind")
    public ResponseEntity<Void> bindFlag(@PathVariable Long id, @RequestBody BindFlagRequest request) {
        adminOpsCounter.increment();
        flagService.bindApplication(id, request.getApplicationId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/applications")
    public ResponseEntity<Application> createApplication(@RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.create(request.getName(), request.getDescription()));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<Application>> listApplications() {
        return ResponseEntity.ok(applicationService.listAll());
    }

    @GetMapping("/flags/{id}/audit")
    public ResponseEntity<List<AuditLog>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getAuditLogs(id));
    }
}
```

- [ ] **Step 3: Create HealthController**

`server/src/main/java/com/ffs/server/controller/HealthController.java`:

```java
package com.ffs.server.controller;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            status.put("database", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
        }
        try {
            redisConnectionFactory.getConnection().ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN: " + e.getMessage());
        }
        boolean allUp = status.values().stream().allMatch(v -> v.equals("UP"));
        return ResponseEntity.status(allUp ? 200 : 503).body(status);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -pl server -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/ffs/server/controller/
git commit -m "feat: add server controllers"
```

---

### Task 7: Server — Tests

**Files:**
- Create: `server/src/test/resources/application-test.yml`
- Create: `server/src/test/resources/schema.sql`
- Create: `server/src/test/java/com/ffs/server/FeatureFlagServerApplicationTests.java`
- Create: `server/src/test/java/com/ffs/server/service/FlagServiceTest.java`
- Create: `server/src/test/java/com/ffs/server/controller/SyncControllerTest.java`

- [ ] **Step 1: Create test application.yml**

`server/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
  cache:
    type: none
  autoconfigure:
    exclude: org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.ffs.server.model.entity
  configuration:
    map-underscore-to-camel-case: true
```

- [ ] **Step 2: Create test schema.sql**

`server/src/test/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(500),
    description TEXT,
    flag_type VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    default_value TEXT NOT NULL DEFAULT 'false',
    enabled BOOLEAN DEFAULT FALSE,
    release_version VARCHAR(100),
    created_by VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flag_application (
    flag_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    PRIMARY KEY (flag_id, application_id),
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS targeting_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id BIGINT NOT NULL,
    priority INT NOT NULL,
    attribute VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    value TEXT NOT NULL,
    serve_value TEXT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rollout (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id BIGINT NOT NULL,
    percentage INT NOT NULL,
    serve_value TEXT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_id BIGINT,
    action VARCHAR(50) NOT NULL,
    changed_by VARCHAR(200),
    detail TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flag_id) REFERENCES flag(id) ON DELETE SET NULL
);
```

- [ ] **Step 3: Create application test bootstrap**

`server/src/test/java/com/ffs/server/FeatureFlagServerApplicationTests.java`:

```java
package com.ffs.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FeatureFlagServerApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Create FlagServiceTest**

`server/src/test/java/com/ffs/server/service/FlagServiceTest.java`:

```java
package com.ffs.server.service;

import com.ffs.server.mapper.ApplicationMapper;
import com.ffs.server.model.dto.CreateFlagRequest;
import com.ffs.server.model.dto.CreateRuleRequest;
import com.ffs.server.model.dto.CreateRolloutRequest;
import com.ffs.server.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlagServiceTest {

    @Autowired
    private FlagService flagService;

    @Autowired
    private ApplicationMapper applicationMapper;

    private Long flagId;

    @BeforeEach
    void setUp() {
        CreateFlagRequest req = new CreateFlagRequest();
        req.setKey("test_flag");
        req.setName("Test Flag");
        req.setFlagType("BOOLEAN");
        req.setDefaultValue("false");
        req.setEnabled(true);
        req.setReleaseVersion("v1.0.0");
        req.setCreatedBy("tester");
        Flag flag = flagService.create(req);
        flagId = flag.getId();
    }

    @Test
    void shouldCreateFlag() {
        Flag found = flagService.getById(flagId);
        assertThat(found.getKey()).isEqualTo("test_flag");
        assertThat(found.getFlagType()).isEqualTo("BOOLEAN");
    }

    @Test
    void shouldAddTargetingRule() {
        CreateRuleRequest req = new CreateRuleRequest();
        req.setPriority(1);
        req.setAttribute("region");
        req.setOperator("EQUALS");
        req.setValue("\"eu-west\"");
        req.setServeValue("true");

        TargetingRule rule = flagService.addRule(flagId, req);
        assertThat(rule.getId()).isNotNull();
        assertThat(rule.getFlagId()).isEqualTo(flagId);
    }

    @Test
    void shouldGetRulesOrderedByPriority() {
        CreateRuleRequest r1 = new CreateRuleRequest();
        r1.setPriority(2); r1.setAttribute("plan"); r1.setOperator("EQUALS");
        r1.setValue("\"premium\""); r1.setServeValue("true");
        flagService.addRule(flagId, r1);

        CreateRuleRequest r2 = new CreateRuleRequest();
        r2.setPriority(1); r2.setAttribute("region"); r2.setOperator("EQUALS");
        r2.setValue("\"eu-west\""); r2.setServeValue("true");
        flagService.addRule(flagId, r2);

        List<TargetingRule> rules = flagService.getRules(flagId);
        assertThat(rules).hasSize(2);
        assertThat(rules.get(0).getPriority()).isEqualTo(1);
    }

    @Test
    void shouldSetRollout() {
        CreateRolloutRequest req = new CreateRolloutRequest();
        req.setPercentage(30);
        req.setServeValue("true");
        flagService.setRollout(flagId, req);

        List<Rollout> rollouts = flagService.getRollouts(flagId);
        assertThat(rollouts).hasSize(1);
        assertThat(rollouts.get(0).getPercentage()).isEqualTo(30);
    }
}
```

- [ ] **Step 5: Create SyncControllerTest**

`server/src/test/java/com/ffs/server/controller/SyncControllerTest.java`:

```java
package com.ffs.server.controller;

import com.ffs.server.mapper.*;
import com.ffs.server.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlagMapper flagMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private TargetingRuleMapper ruleMapper;

    @Autowired
    private FlagMapper flagMapperForBind;

    @BeforeEach
    void setUp() {
        Application app = new Application("test-app", "Test");
        applicationMapper.insert(app);

        Flag flag = new Flag();
        flag.setKey("feature_x");
        flag.setName("Feature X");
        flag.setFlagType("BOOLEAN");
        flag.setDefaultValue("false");
        flag.setEnabled(true);
        flagMapper.insert(flag);

        flagMapperForBind.insertFlagApplication(flag.getId(), app.getId());

        TargetingRule rule = new TargetingRule();
        rule.setFlagId(flag.getId());
        rule.setPriority(1);
        rule.setAttribute("region");
        rule.setOperator("EQUALS");
        rule.setValue("\"eu-west\"");
        rule.setServeValue("true");
        rule.setEnabled(true);
        ruleMapper.insert(rule);
    }

    @Test
    void shouldSyncAllFlagsForApp() throws Exception {
        mockMvc.perform(get("/api/v1/eval/sync").param("appId", "test-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.length()").value(1))
                .andExpect(jsonPath("$.flags[0].key").value("feature_x"))
                .andExpect(jsonPath("$.flags[0].rules[0].attribute").value("region"));
    }

    @Test
    void shouldReturnErrorForUnknownApp() throws Exception {
        mockMvc.perform(get("/api/v1/eval/sync").param("appId", "nonexistent"))
                .andExpect(status().is5xxServerError());
    }
}
```

- [ ] **Step 6: Run server tests**

Run: `mvn test -pl server -am`
Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add server/src/test/
git commit -m "test: add server tests with MyBatis and H2"
```

---

### Task 8: SDK — Models

**Files:**
- Create: `sdk/src/main/java/com/ffs/sdk/model/FFUser.java`
- Create: `sdk/src/main/java/com/ffs/sdk/model/FlagConfig.java`
- Create: `sdk/src/main/java/com/ffs/sdk/model/TargetingRule.java`
- Create: `sdk/src/main/java/com/ffs/sdk/model/EvalResult.java`
- Create: `sdk/src/main/java/com/ffs/sdk/model/TraceEntry.java`

- [ ] **Step 1: Create FFUser**

`sdk/src/main/java/com/ffs/sdk/model/FFUser.java`:

```java
package com.ffs.sdk.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FFUser {
    private final String id;
    private final Map<String, String> attributes;

    private FFUser(Builder builder) {
        this.id = builder.id;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    public String getId() { return id; }
    public String getAttribute(String key) { return attributes.get(key); }
    public Map<String, String> getAttributes() { return attributes; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private final Map<String, String> attributes = new HashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder region(String region) { attributes.put("region", region); return this; }
        public Builder country(String country) { attributes.put("country", country); return this; }
        public Builder plan(String plan) { attributes.put("plan", plan); return this; }
        public Builder email(String email) { attributes.put("email", email); return this; }
        public Builder custom(String key, String value) { attributes.put(key, value); return this; }

        public FFUser build() {
            if (id == null) throw new IllegalArgumentException("id is required");
            return new FFUser(this);
        }
    }
}
```

- [ ] **Step 2: Create SDK models**

`sdk/src/main/java/com/ffs/sdk/model/TargetingRule.java`:

```java
package com.ffs.sdk.model;

public class TargetingRule {
    private String id;
    private int priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;

    public TargetingRule() {}
    public TargetingRule(int priority, String attribute, String operator, String value, String serveValue) {
        this.priority = priority; this.attribute = attribute; this.operator = operator;
        this.value = value; this.serveValue = serveValue;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
}
```

`sdk/src/main/java/com/ffs/sdk/model/FlagConfig.java`:

```java
package com.ffs.sdk.model;

import java.util.List;

public class FlagConfig {
    private String key;
    private String type;
    private String defaultValue;
    private boolean enabled;
    private String releaseVersion;
    private String updatedAt;
    private List<TargetingRule> rules;
    private Rollout rollout;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<TargetingRule> getRules() { return rules; }
    public void setRules(List<TargetingRule> rules) { this.rules = rules; }
    public Rollout getRollout() { return rollout; }
    public void setRollout(Rollout rollout) { this.rollout = rollout; }

    public static class Rollout {
        private int percentage;
        private String serveValue;
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
        public String getServeValue() { return serveValue; }
        public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    }
}
```

`sdk/src/main/java/com/ffs/sdk/model/TraceEntry.java`:

```java
package com.ffs.sdk.model;

public class TraceEntry {
    private String ruleId;
    private String condition;
    private boolean matched;

    public TraceEntry() {}
    public TraceEntry(String ruleId, String condition, boolean matched) {
        this.ruleId = ruleId; this.condition = condition; this.matched = matched;
    }
    public String getRuleId() { return ruleId; }
    public String getCondition() { return condition; }
    public boolean isMatched() { return matched; }
}
```

`sdk/src/main/java/com/ffs/sdk/model/EvalResult.java`:

```java
package com.ffs.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class EvalResult {
    private String flagKey;
    private String value;
    private String reason;
    private String matchedRuleId;
    private List<TraceEntry> trace = new ArrayList<>();
    private String releaseVersion;
    private String flagUpdatedAt;

    public EvalResult() {}
    public EvalResult(String flagKey, String value, String reason) {
        this.flagKey = flagKey; this.value = value; this.reason = reason;
    }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMatchedRuleId() { return matchedRuleId; }
    public void setMatchedRuleId(String matchedRuleId) { this.matchedRuleId = matchedRuleId; }
    public List<TraceEntry> getTrace() { return trace; }
    public void setTrace(List<TraceEntry> trace) { this.trace = trace; }
    public void addTrace(TraceEntry entry) { this.trace.add(entry); }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getFlagUpdatedAt() { return flagUpdatedAt; }
    public void setFlagUpdatedAt(String flagUpdatedAt) { this.flagUpdatedAt = flagUpdatedAt; }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -pl sdk -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add sdk/src/main/java/com/ffs/sdk/model/
git commit -m "feat: add SDK models"
```

---

### Task 9: SDK — RuleEvaluator

**Files:**
- Create: `sdk/src/main/java/com/ffs/sdk/evaluator/RuleEvaluator.java`
- Create: `sdk/src/test/java/com/ffs/sdk/evaluator/RuleEvaluatorTest.java`

- [ ] **Step 1: Create RuleEvaluator**

`sdk/src/main/java/com/ffs/sdk/evaluator/RuleEvaluator.java`:

```java
package com.ffs.sdk.evaluator;

import com.ffs.sdk.model.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class RuleEvaluator {

    public EvalResult evaluate(FlagConfig flag, FFUser user) {
        if (!flag.isEnabled()) {
            return buildResult(flag, flag.getDefaultValue(), "disabled", null);
        }
        if (flag.getRules() != null) {
            for (TargetingRule rule : flag.getRules()) {
                if (evaluateRule(rule, user)) {
                    return buildResult(flag, rule.getServeValue(), "rule_match",
                            String.valueOf(rule.getPriority()));
                }
            }
        }
        if (flag.getRollout() != null && flag.getRollout().getPercentage() > 0) {
            if (isInRollout(user.getId(), flag.getKey(), flag.getRollout().getPercentage())) {
                return buildResult(flag, flag.getRollout().getServeValue(), "rollout", null);
            }
        }
        return buildResult(flag, flag.getDefaultValue(), "default", null);
    }

    private boolean evaluateRule(TargetingRule rule, FFUser user) {
        String userValue = user.getAttribute(rule.getAttribute());
        String ruleValue = stripQuotes(rule.getValue());
        switch (rule.getOperator().toUpperCase()) {
            case "EQUALS": return ruleValue.equals(userValue);
            case "IN":
                List<String> values = parseList(rule.getValue());
                return userValue != null && values.contains(userValue);
            case "CONTAINS":
                return userValue != null && Arrays.asList(userValue.split(",")).contains(ruleValue);
            case "GREATER_THAN":
                return userValue != null && safeCompare(userValue, ruleValue) > 0;
            case "LESS_THAN":
                return userValue != null && safeCompare(userValue, ruleValue) < 0;
            default: return false;
        }
    }

    private int safeCompare(String a, String b) {
        try { return Double.compare(Double.parseDouble(a), Double.parseDouble(b)); }
        catch (NumberFormatException e) { return 0; }
    }

    private boolean isInRollout(String userId, String flagKey, int percentage) {
        if (userId == null) return false;
        int hash = Math.abs(murmurHash((flagKey + ":" + userId).getBytes(StandardCharsets.UTF_8)));
        return (hash % 100) < percentage;
    }

    private int murmurHash(byte[] data) {
        int h = 0x9747b28c;
        for (byte b : data) { h ^= (b & 0xFF); h *= 0x5bd1e995; h ^= h >>> 15; }
        return h;
    }

    private String stripQuotes(String v) {
        if (v == null) return null;
        String t = v.trim();
        return (t.startsWith("\"") && t.endsWith("\"")) ? t.substring(1, t.length() - 1) : t;
    }

    private List<String> parseList(String v) {
        String t = v.trim();
        if (t.startsWith("[") && t.endsWith("]")) {
            return Arrays.stream(t.substring(1, t.length() - 1).split(","))
                    .map(String::trim).map(this::stripQuotes).toList();
        }
        return List.of(stripQuotes(v));
    }

    private EvalResult buildResult(FlagConfig flag, String value, String reason, String matchedRuleId) {
        EvalResult r = new EvalResult(flag.getKey(), value, reason);
        r.setMatchedRuleId(matchedRuleId);
        r.setReleaseVersion(flag.getReleaseVersion());
        r.setFlagUpdatedAt(flag.getUpdatedAt());
        if (flag.getRules() != null) {
            for (TargetingRule rule : flag.getRules()) {
                r.addTrace(new TraceEntry(String.valueOf(rule.getPriority()),
                        rule.getAttribute() + " " + rule.getOperator() + " " + rule.getValue(),
                        String.valueOf(rule.getPriority()).equals(matchedRuleId)));
            }
        }
        if (flag.getRollout() != null) {
            r.addTrace(new TraceEntry("rollout", "rollout " + flag.getRollout().getPercentage() + "%",
                    "rollout".equals(reason)));
        }
        r.addTrace(new TraceEntry("default", "default: " + flag.getDefaultValue(), "default".equals(reason)));
        return r;
    }
}
```

- [ ] **Step 2: Create RuleEvaluatorTest**

`sdk/src/test/java/com/ffs/sdk/evaluator/RuleEvaluatorTest.java`:

```java
package com.ffs.sdk.evaluator;

import com.ffs.sdk.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {
    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() { evaluator = new RuleEvaluator(); }

    @Test
    void shouldReturnDefaultWhenDisabled() {
        FlagConfig f = flag("test", "false", false);
        assertThat(evaluator.evaluate(f, user("u1")).getValue()).isEqualTo("false");
        assertThat(evaluator.evaluate(f, user("u1")).getReason()).isEqualTo("disabled");
    }

    @Test
    void shouldMatchEqualsRule() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(new TargetingRule(1, "region", "EQUALS", "\"eu-west\"", "true")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").region("eu-west").build());
        assertThat(r.getValue()).isEqualTo("true");
        assertThat(r.getReason()).isEqualTo("rule_match");
    }

    @Test
    void shouldRespectPriority() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(
                new TargetingRule(1, "region", "EQUALS", "\"eu-west\"", "true"),
                new TargetingRule(2, "region", "EQUALS", "\"eu-west\"", "false")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").region("eu-west").build());
        assertThat(r.getValue()).isEqualTo("true");
    }

    @Test
    void shouldHandle100PercentRollout() {
        FlagConfig f = flag("test", "false", true);
        FlagConfig.Rollout ro = new FlagConfig.Rollout(); ro.setPercentage(100); ro.setServeValue("true");
        f.setRollout(ro);
        assertThat(evaluator.evaluate(f, user("u1")).getReason()).isEqualTo("rollout");
    }

    @Test
    void shouldMatchCustomAttribute() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(new TargetingRule(1, "membership_level", "EQUALS", "\"vip\"", "true")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").custom("membership_level", "vip").build());
        assertThat(r.getValue()).isEqualTo("true");
    }

    @Test
    void shouldProduceTrace() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(new TargetingRule(1, "region", "EQUALS", "\"eu-west\"", "true")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").region("eu-west").build());
        assertThat(r.getTrace()).isNotEmpty();
        assertThat(r.getTrace().get(0).isMatched()).isTrue();
    }

    private FlagConfig flag(String key, String defVal, boolean enabled) {
        FlagConfig f = new FlagConfig();
        f.setKey(key); f.setType("BOOLEAN"); f.setDefaultValue(defVal); f.setEnabled(enabled);
        return f;
    }

    private FFUser user(String id) { return FFUser.builder().id(id).build(); }
}
```

- [ ] **Step 3: Run SDK tests**

Run: `mvn test -pl sdk -am`
Expected: All 6 tests pass

- [ ] **Step 4: Commit**

```bash
git add sdk/src/main/java/com/ffs/sdk/evaluator/ sdk/src/test/
git commit -m "feat: add SDK RuleEvaluator with tests"
```

---

### Task 10: SDK — Cache, Poller, Client

**Files:**
- Create: `sdk/src/main/java/com/ffs/sdk/cache/FlagCache.java`
- Create: `sdk/src/main/java/com/ffs/sdk/sync/ConfigPoller.java`
- Create: `sdk/src/main/java/com/ffs/sdk/config/ClientConfig.java`
- Create: `sdk/src/main/java/com/ffs/sdk/FeatureFlagClientBuilder.java`
- Create: `sdk/src/main/java/com/ffs/sdk/FeatureFlagClient.java`

- [ ] **Step 1: Create FlagCache**

`sdk/src/main/java/com/ffs/sdk/cache/FlagCache.java`:

```java
package com.ffs.sdk.cache;

import com.ffs.sdk.model.FlagConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagCache {
    private volatile Map<String, FlagConfig> flags = new ConcurrentHashMap<>();
    private volatile long lastSyncTime = 0;

    public FlagConfig get(String key) { return flags.get(key); }

    public void putAll(Map<String, FlagConfig> newFlags) {
        this.flags = new ConcurrentHashMap<>(newFlags);
        this.lastSyncTime = System.currentTimeMillis();
    }

    public void merge(Map<String, FlagConfig> updated) {
        this.flags.putAll(updated);
        this.lastSyncTime = System.currentTimeMillis();
    }

    public int size() { return flags.size(); }
    public boolean isEmpty() { return flags.isEmpty(); }
    public long cacheAgeMs() { return System.currentTimeMillis() - lastSyncTime; }
}
```

- [ ] **Step 2: Create ConfigPoller**

`sdk/src/main/java/com/ffs/sdk/sync/ConfigPoller.java`:

```java
package com.ffs.sdk.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.model.FlagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigPoller {
    private static final Logger log = LoggerFactory.getLogger(ConfigPoller.class);

    private final String serverUrl;
    private final String appId;
    private final long intervalMs;
    private final FlagCache cache;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ScheduledExecutorService scheduler;
    private Instant lastSyncTimestamp;
    private volatile boolean running = false;

    public ConfigPoller(String serverUrl, String appId, long intervalMs, FlagCache cache) {
        this.serverUrl = serverUrl;
        this.appId = appId;
        this.intervalMs = intervalMs;
        this.cache = cache;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        syncFull();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ff-sdk-poller");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::syncIncremental, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdown();
    }

    private void syncFull() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/v1/eval/sync?appId=" + appId))
                    .GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                cache.putAll(parseFlags(resp.body()));
                lastSyncTimestamp = Instant.now();
                log.info("SDK initial sync: {} flags", cache.size());
            }
        } catch (Exception e) { log.warn("SDK initial sync failed: {}", e.getMessage()); }
    }

    private void syncIncremental() {
        try {
            String url = serverUrl + "/api/v1/eval/sync?appId=" + appId;
            if (lastSyncTimestamp != null) url += "&since=" + lastSyncTimestamp.toString();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, FlagConfig> updated = parseFlags(resp.body());
                if (!updated.isEmpty()) { cache.merge(updated); log.debug("SDK sync updated {} flags", updated.size()); }
                lastSyncTimestamp = Instant.now();
            }
        } catch (Exception e) { log.warn("SDK sync error: {}", e.getMessage()); }
    }

    private Map<String, FlagConfig> parseFlags(String body) {
        Map<String, FlagConfig> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode arr = root.get("flags");
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) {
                    FlagConfig f = objectMapper.treeToValue(n, FlagConfig.class);
                    result.put(f.getKey(), f);
                }
            }
        } catch (Exception e) { log.warn("SDK parse error: {}", e.getMessage()); }
        return result;
    }
}
```

- [ ] **Step 3: Create ClientConfig**

`sdk/src/main/java/com/ffs/sdk/config/ClientConfig.java`:

```java
package com.ffs.sdk.config;

import java.time.Duration;

public class ClientConfig {
    private final String serverUrl;
    private final String appId;
    private final long syncIntervalMs;

    private ClientConfig(Builder b) {
        this.serverUrl = b.serverUrl; this.appId = b.appId; this.syncIntervalMs = b.syncIntervalMs;
    }

    public String getServerUrl() { return serverUrl; }
    public String getAppId() { return appId; }
    public long getSyncIntervalMs() { return syncIntervalMs; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String serverUrl = "http://localhost:8080";
        private String appId;
        private long syncIntervalMs = 10_000;

        public Builder serverUrl(String v) { this.serverUrl = v; return this; }
        public Builder appId(String v) { this.appId = v; return this; }
        public Builder syncInterval(Duration d) { this.syncIntervalMs = d.toMillis(); return this; }

        public ClientConfig build() {
            if (appId == null) throw new IllegalArgumentException("appId is required");
            return new ClientConfig(this);
        }
    }
}
```

- [ ] **Step 4: Create FeatureFlagClientBuilder**

`sdk/src/main/java/com/ffs/sdk/FeatureFlagClientBuilder.java`:

```java
package com.ffs.sdk;

import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.config.ClientConfig;
import com.ffs.sdk.sync.ConfigPoller;
import java.time.Duration;

public class FeatureFlagClientBuilder {
    private String serverUrl = "http://localhost:8080";
    private String appId;
    private Duration syncInterval = Duration.ofSeconds(10);

    public FeatureFlagClientBuilder serverUrl(String v) { this.serverUrl = v; return this; }
    public FeatureFlagClientBuilder appId(String v) { this.appId = v; return this; }
    public FeatureFlagClientBuilder syncInterval(Duration v) { this.syncInterval = v; return this; }

    public FeatureFlagClient build() {
        if (appId == null) throw new IllegalArgumentException("appId is required");
        ClientConfig config = ClientConfig.builder().serverUrl(serverUrl).appId(appId).syncInterval(syncInterval).build();
        FlagCache cache = new FlagCache();
        ConfigPoller poller = new ConfigPoller(config.getServerUrl(), config.getAppId(), config.getSyncIntervalMs(), cache);
        return new FeatureFlagClient(config, cache, poller);
    }
}
```

- [ ] **Step 5: Create FeatureFlagClient**

`sdk/src/main/java/com/ffs/sdk/FeatureFlagClient.java`:

```java
package com.ffs.sdk;

import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.config.ClientConfig;
import com.ffs.sdk.evaluator.RuleEvaluator;
import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import com.ffs.sdk.model.FlagConfig;
import com.ffs.sdk.sync.ConfigPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureFlagClient {
    private static final Logger log = LoggerFactory.getLogger(FeatureFlagClient.class);
    private final FlagCache cache;
    private final ConfigPoller poller;
    private final RuleEvaluator evaluator = new RuleEvaluator();

    FeatureFlagClient(ClientConfig config, FlagCache cache, ConfigPoller poller) {
        this.cache = cache; this.poller = poller;
    }

    public static FeatureFlagClientBuilder builder() { return new FeatureFlagClientBuilder(); }
    public void start() { poller.start(); }
    public void close() { poller.stop(); }

    public boolean isEnabled(String flagKey, FFUser user) {
        return "true".equalsIgnoreCase(evaluate(flagKey, user, "false"));
    }

    public String stringValue(String flagKey, FFUser user, String defaultValue) {
        return evaluate(flagKey, user, defaultValue);
    }

    public int intValue(String flagKey, FFUser user, int defaultValue) {
        try { return Integer.parseInt(evaluate(flagKey, user, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public Map<String, EvalResult> evaluateAll(FFUser user, String... flagKeys) {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        for (String key : flagKeys) {
            FlagConfig flag = cache.get(key);
            results.put(key, flag != null ? evaluator.evaluate(flag, user)
                    : new EvalResult(key, "false", "not_found"));
        }
        return results;
    }

    private String evaluate(String flagKey, FFUser user, String defaultValue) {
        FlagConfig flag = cache.get(flagKey);
        if (flag == null) return defaultValue;
        return evaluator.evaluate(flag, user).getValue();
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `mvn clean compile -pl sdk -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add sdk/src/main/java/com/ffs/sdk/
git commit -m "feat: add SDK client — cache, poller, FeatureFlagClient"
```

---

### Task 11: Cross-platform SDK Spec

**Files:**
- Create: `sdk-spec/sdk-interface.md`
- Create: `sdk-spec/test-cases.json`

- [ ] **Step 1: Create sdk-interface.md**

`sdk-spec/sdk-interface.md`:

```markdown
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
```

- [ ] **Step 2: Create test-cases.json**

`sdk-spec/test-cases.json`:

```json
{
  "version": "1.0",
  "testCases": [
    {
      "name": "disabled flag returns default",
      "flag": { "key": "test", "type": "BOOLEAN", "defaultValue": "false", "enabled": false, "rules": [] },
      "user": { "id": "u1" },
      "expected": "false", "expectedReason": "disabled"
    },
    {
      "name": "rule EQUALS match",
      "flag": { "key": "test", "type": "BOOLEAN", "defaultValue": "false", "enabled": true,
        "rules": [{ "priority": 1, "attribute": "region", "operator": "EQUALS", "value": "\"eu-west\"", "serveValue": "true" }] },
      "user": { "id": "u1", "region": "eu-west" },
      "expected": "true", "expectedReason": "rule_match"
    },
    {
      "name": "rule priority first match wins",
      "flag": { "key": "test", "type": "BOOLEAN", "defaultValue": "false", "enabled": true,
        "rules": [
          { "priority": 1, "attribute": "region", "operator": "EQUALS", "value": "\"eu-west\"", "serveValue": "true" },
          { "priority": 2, "attribute": "region", "operator": "EQUALS", "value": "\"eu-west\"", "serveValue": "false" }
        ] },
      "user": { "id": "u1", "region": "eu-west" },
      "expected": "true", "expectedReason": "rule_match"
    },
    {
      "name": "rollout 100%",
      "flag": { "key": "test", "type": "BOOLEAN", "defaultValue": "false", "enabled": true, "rules": [],
        "rollout": { "percentage": 100, "serveValue": "true" } },
      "user": { "id": "u1" },
      "expected": "true", "expectedReason": "rollout"
    },
    {
      "name": "custom attribute targeting",
      "flag": { "key": "test", "type": "BOOLEAN", "defaultValue": "false", "enabled": true,
        "rules": [{ "priority": 1, "attribute": "membership_level", "operator": "EQUALS", "value": "\"vip\"", "serveValue": "true" }] },
      "user": { "id": "u1", "membership_level": "vip" },
      "expected": "true", "expectedReason": "rule_match"
    }
  ]
}
```

- [ ] **Step 3: Commit**

```bash
git add sdk-spec/
git commit -m "docs: add cross-platform SDK spec and test cases"
```

---

### Task 12: Sample App

**Files:**
- Create: `sample/src/main/java/com/ffs/sample/SampleApplication.java`
- Create: `sample/src/main/java/com/ffs/sample/controller/DemoController.java`
- Create: `sample/src/main/resources/application.yml`

- [ ] **Step 1: Create SampleApplication**

`sample/src/main/java/com/ffs/sample/SampleApplication.java`:

```java
package com.ffs.sample;

import com.ffs.sdk.FeatureFlagClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Duration;

@SpringBootApplication
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    public FeatureFlagClient featureFlagClient() {
        String serverUrl = System.getenv().getOrDefault("FF_SERVER_URL", "http://localhost:8080");
        String appId = System.getenv().getOrDefault("FF_APP_ID", "sample-app");
        FeatureFlagClient client = FeatureFlagClient.builder()
                .serverUrl(serverUrl).appId(appId).syncInterval(Duration.ofSeconds(10)).build();
        client.start();
        return client;
    }
}
```

- [ ] **Step 2: Create DemoController**

`sample/src/main/java/com/ffs/sample/controller/DemoController.java`:

```java
package com.ffs.sample.controller;

import com.ffs.sdk.FeatureFlagClient;
import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private final FeatureFlagClient ffClient;

    public DemoController(FeatureFlagClient ffClient) { this.ffClient = ffClient; }

    @GetMapping("/checkout")
    public Map<String, Object> checkout(
            @RequestParam(defaultValue = "user_1") String userId,
            @RequestParam(defaultValue = "eu-west") String region,
            @RequestParam(defaultValue = "premium") String plan) {
        FFUser user = FFUser.builder().id(userId).region(region).plan(plan).build();
        boolean newCheckout = ffClient.isEnabled("new_checkout_ui", user);
        boolean darkMode = ffClient.isEnabled("dark_mode", user);
        int maxResults = ffClient.intValue("max_search_results", user, 20);

        Map<String, Object> resp = new HashMap<>();
        resp.put("user_id", userId); resp.put("region", region); resp.put("plan", plan);
        resp.put("new_checkout_ui", newCheckout);
        resp.put("dark_mode", darkMode);
        resp.put("max_search_results", maxResults);

        Map<String, EvalResult> details = ffClient.evaluateAll(user,
                "new_checkout_ui", "dark_mode", "max_search_results");
        resp.put("explainability", details);
        log.info("Checkout evaluation: {}", details);
        return resp;
    }

    @GetMapping("/explain/{flagKey}")
    public Map<String, Object> explain(@PathVariable String flagKey,
            @RequestParam(defaultValue = "user_1") String userId,
            @RequestParam(defaultValue = "eu-west") String region,
            @RequestParam(defaultValue = "premium") String plan) {
        FFUser user = FFUser.builder().id(userId).region(region).plan(plan).build();
        Map<String, EvalResult> results = ffClient.evaluateAll(user, flagKey);
        EvalResult result = results.get(flagKey);
        Map<String, Object> resp = new HashMap<>();
        if (result != null) {
            resp.put("flag", result.getFlagKey()); resp.put("value", result.getValue());
            resp.put("reason", result.getReason()); resp.put("trace", result.getTrace());
            resp.put("releaseVersion", result.getReleaseVersion());
        } else { resp.put("error", "Flag not found"); }
        return resp;
    }
}
```

- [ ] **Step 3: Create sample application.yml**

`sample/src/main/resources/application.yml`:

```yaml
server:
  port: 8081
logging:
  level:
    com.ffs: DEBUG
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -pl sample -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add sample/
git commit -m "feat: add sample app"
```

---

### Task 13: README & Final Verification

**Files:**
- Create: `README.md`
- Create: `.gitignore`

- [ ] **Step 1: Create files**

`README.md`:

```markdown
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
```

`.gitignore`:

```gitignore
target/
*.class
*.jar
*.log
.idea/
*.iml
.vscode/
.DS_Store
```

- [ ] **Step 2: Full build**

Run: `mvn clean package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Run all tests**

Run: `mvn test`
Expected: ALL TESTS PASS

- [ ] **Step 4: Install SDK JAR to local Maven repo**

Run: `mvn install -pl sdk -am -DskipTests`
Expected: BUILD SUCCESS, JAR in ~/.m2

- [ ] **Step 5: Start with Docker Compose & smoke test**

Run:
```bash
docker-compose up -d
sleep 30
curl -s http://localhost:8080/api/v1/health
curl -s "http://localhost:8081/demo/checkout?userId=user_1&region=eu-west&plan=premium"
```

Expected: health returns UP/UP, checkout returns new_checkout_ui=true

- [ ] **Step 6: Tear down & commit**

```bash
docker-compose down
git add README.md .gitignore
git commit -m "docs: add README, final verification complete"
```
