CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

INSERT INTO categories (id, name)
VALUES
    ('9d8aafd4-9222-4150-aadb-5167405a7720', 'Education'),
    ('9d8aafd4-9222-4150-aadb-5167405a7721', 'Entertainment');
