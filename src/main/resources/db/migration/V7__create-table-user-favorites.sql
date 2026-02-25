CREATE TABLE user_favorites (
                                id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                                user_id UUID NOT NULL,
                                item_id VARCHAR(255) NOT NULL,
                                item_type VARCHAR(20) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT uk_user_item_type UNIQUE (user_id, item_id, item_type),
                                FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);