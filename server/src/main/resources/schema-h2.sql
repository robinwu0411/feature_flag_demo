CREATE TABLE IF NOT EXISTS flag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(500),
    description TEXT,
    flag_type VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    default_value VARCHAR(500) NOT NULL DEFAULT 'false',
    enabled BOOLEAN DEFAULT FALSE,
    release_version VARCHAR(100),
    app_name VARCHAR(100) NOT NULL,
    created_by VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO flag (id, `key`, name, flag_type, default_value, enabled, release_version, app_name, created_by)
VALUES (1, 'new_checkout_ui', 'New Checkout UI', 'BOOLEAN', 'false', TRUE, 'v3.2.0', 'sample-app', 'admin');
INSERT INTO flag (id, `key`, name, flag_type, default_value, enabled, release_version, app_name, created_by)
VALUES (2, 'dark_mode', 'Dark Mode Theme', 'BOOLEAN', 'false', TRUE, 'v3.1.0', 'sample-app', 'admin');
INSERT INTO flag (id, `key`, name, flag_type, default_value, enabled, release_version, app_name, created_by)
VALUES (3, 'max_search_results', 'Max Search Results', 'NUMBER', '20', TRUE, 'v3.0.0', 'sample-app', 'admin');
