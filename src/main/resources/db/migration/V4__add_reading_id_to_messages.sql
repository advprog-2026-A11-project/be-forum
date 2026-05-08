ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS reading_id VARCHAR (128);

CREATE INDEX IF NOT EXISTS idx_messages_reading_id ON messages(reading_id);
