ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD COLUMN queue_position INTEGER NULL;

UPDATE users
SET status = 'ACTIVE',
    queue_position = NULL
WHERE status IS NULL;

CREATE TABLE app_config (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL
);

INSERT INTO app_config (config_key, config_value)
VALUES ('MAX_ACTIVE_USERS', '100');
