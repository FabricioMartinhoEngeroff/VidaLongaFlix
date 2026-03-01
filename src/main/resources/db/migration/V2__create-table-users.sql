CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    tax_id VARCHAR(14) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL,
    street VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(2) NOT NULL,  -- Representa o Enum State
    postal_code VARCHAR(9) NOT NULL,
    photo VARCHAR(255),
    profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO users (
    id, name, email, password, tax_id, phone,
    street, neighborhood, city, state, postal_code,
    photo, profile_complete, created_at, updated_at
) VALUES (
    'd193afd4-9222-4150-aadb-5167405a771c',
    'newuser',
    'newuser@example.com',
    '$2a$10$3yHNGw3SkUYZECFGm3N9tOmXWQiS.K5/VYj3wVlTZzDrMGo5q6fRu',
    '123.456.789-00',
    '(51)91234-5678',
    'Rua das Flores, 123',
    'Centro',
    'Bom Princípio',
    'RS',
    '95780-000',
    NULL,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Usuário ADMIN para testar rotas /admin/**
-- Senha: Admin@123456
INSERT INTO users (
    id, name, email, password, tax_id, phone,
    street, neighborhood, city, state, postal_code,
    photo, profile_complete, created_at, updated_at
) VALUES (
             'e293afd4-9222-4150-aadb-5167405a771d',
             'Admin User',
             'admin@vidalongaflix.com',
             '$2a$10$3yHNGw3SkUYZECFGm3N9tOmXWQiS.K5/VYj3wVlTZzDrMGo5q6fRu',
             '987.654.321-00',
             '(51)99999-9999',
             'Rua Admin, 1',
             'Centro',
             'Bom Princípio',
             'RS',
             '95780-000',
             NULL,
             TRUE,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         );
