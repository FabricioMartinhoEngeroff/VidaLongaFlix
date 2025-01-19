CREATE TABLE users (
    id UUID PRIMARY KEY,
    login VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

INSERT INTO users (id, login, email, password)
VALUES (
    'd193afd4-9222-4150-aadb-5167405a771c',
    'newuser',
    'newuser@example.com',
    '$2a$10$3yHNGw3SkUYZECFGm3N9tOmXWQiS.K5/VYj3wVlTZzDrMGo5q6fRu'
);