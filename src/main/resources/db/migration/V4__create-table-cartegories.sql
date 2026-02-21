CREATE TABLE categories (
                            id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            type VARCHAR(20) NOT NULL DEFAULT 'VIDEO',
                            CONSTRAINT categories_name_type_unique UNIQUE (name, type)
);

INSERT INTO categories (id, name, type) VALUES
                                            ('9d8aafd4-9222-4150-aadb-5167405a7720', 'Education', 'VIDEO'),
                                            ('9d8aafd4-9222-4150-aadb-5167405a7721', 'Entertainment', 'VIDEO');