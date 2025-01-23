CREATE TABLE comments (
    id UUID PRIMARY KEY,
    text VARCHAR(500) NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id UUID NOT NULL,
    video_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE
);

INSERT INTO comments (id, text, date, user_id, video_id)
VALUES
('c23e4567-e89b-12d3-a456-426614174004', 'Nice video!', NOW(), 'd193afd4-9222-4150-aadb-5167405a771c', 'a23e4567-e89b-12d3-a456-426614174002'),
('c23e4567-e89b-12d3-a456-426614174005', 'Great content!', NOW(), 'd193afd4-9222-4150-aadb-5167405a771c', 'a23e4567-e89b-12d3-a456-426614174003');
