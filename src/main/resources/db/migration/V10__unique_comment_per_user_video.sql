ALTER TABLE comments
    ADD CONSTRAINT uk_comments_user_video UNIQUE (user_id, video_id);

