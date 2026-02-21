CREATE TABLE videos (
                        id UUID PRIMARY KEY NOT NULL,
                        title VARCHAR(150) NOT NULL,
                        description TEXT NOT NULL,
                        url VARCHAR(255) NOT NULL,
                        thumbnail_url VARCHAR(255) NOT NULL DEFAULT '',
                        category_id UUID NOT NULL,
                        views INT NOT NULL DEFAULT 0,
                        watch_time DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                        recipe TEXT,
                        protein DOUBLE PRECISION,
                        carbs DOUBLE PRECISION,
                        fat DOUBLE PRECISION,
                        fiber DOUBLE PRECISION,
                        calories DOUBLE PRECISION,
                        likes_count INT NOT NULL DEFAULT 0,
                        favorited BOOLEAN NOT NULL DEFAULT FALSE,
                        FOREIGN KEY (category_id) REFERENCES categories (id)
);

INSERT INTO videos (id, title, description, url, thumbnail_url, category_id, views, watch_time, recipe, protein, carbs, fat, fiber, calories, likes_count, favorited)
VALUES
    ('a23e4567-e89b-12d3-a456-426614174002',
     'Bolo de Banana Fit', 'Receita saudável de bolo de banana',
     'http://example.com/video1', '', '9d8aafd4-9222-4150-aadb-5167405a7720',
     100, 12.5, NULL, NULL, NULL, NULL, NULL, NULL, 0, FALSE),

    ('a23e4567-e89b-12d3-a456-426614174003',
     'Salada Proteica', 'Salada rica em proteínas para o almoço',
     'http://example.com/video2', '', '9d8aafd4-9222-4150-aadb-5167405a7721',
     50, 8.0, NULL, NULL, NULL, NULL, NULL, NULL, 0, FALSE);