CREATE TABLE users_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

INSERT INTO users_roles (user_id, role_id)
VALUES ('d193afd4-9222-4150-aadb-5167405a771c', '123e4567-e89b-12d3-a456-426614174000');