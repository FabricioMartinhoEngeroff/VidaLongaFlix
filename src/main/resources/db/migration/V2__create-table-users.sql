CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    telefone VARCHAR(15) NOT NULL,
    rua VARCHAR(255) NOT NULL,
    bairro VARCHAR(255) NOT NULL,
    cidade VARCHAR(255) NOT NULL,
    estado VARCHAR(2) NOT NULL,  -- Representa o Enum Estado
    cep VARCHAR(9) NOT NULL
);

INSERT INTO users (
    id, name, email, password, cpf, telefone,
    rua, bairro, cidade, estado, cep
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
    '95780-000'
);