-- Tabela de mensagens WhatsApp Business enviadas pela plataforma
CREATE TABLE IF NOT EXISTS message (
    id               BIGSERIAL       PRIMARY KEY,
    destination      VARCHAR(20)     NOT NULL,
    title            VARCHAR(255),
    body             VARCHAR(255),
    delivery_status  VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX IF NOT EXISTS idx_message_destination ON message (destination);
CREATE INDEX IF NOT EXISTS idx_message_delivery_status ON message (delivery_status);
