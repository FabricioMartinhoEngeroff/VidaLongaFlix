CREATE TABLE videos (
    id UUID PRIMARY KEY NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    url VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL,
    views INT NOT NULL DEFAULT 0,
    watch_time DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories (id)
);

INSERT INTO videos (id, title, description, url, category_id, views, watch_time)
VALUES
    ('a23e4567-e89b-12d3-a456-426614174002', 'Video 1', 'Description for Video 1', 'http://example.com/video1', '9d8aafd4-9222-4150-aadb-5167405a7720', 100, 12.5),
    ('a23e4567-e89b-12d3-a456-426614174003', 'Video 2', 'Description for Video 2', 'http://example.com/video2', '9d8aafd4-9222-4150-aadb-5167405a7721', 50, 8.0);
