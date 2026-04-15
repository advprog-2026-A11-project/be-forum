ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS user_id UUID;

CREATE INDEX IF NOT EXISTS idx_messages_user_id ON messages(user_id);
