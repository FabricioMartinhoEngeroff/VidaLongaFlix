CREATE TABLE notifications (
    id         UUID         PRIMARY KEY,
    type       VARCHAR(10)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content_id UUID         NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);
