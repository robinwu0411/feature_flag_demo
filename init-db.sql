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
