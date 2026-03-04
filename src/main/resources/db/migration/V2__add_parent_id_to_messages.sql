-- Add parent_id column for reply functionality (self-referential relationship)
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS parent_id UUID;

-- Add foreign key constraint to reference the parent message
ALTER TABLE messages
    ADD CONSTRAINT fk_messages_parent
        FOREIGN KEY (parent_id) REFERENCES messages (id) ON DELETE CASCADE;

-- Create index for faster lookup of replies by parent_id
CREATE INDEX IF NOT EXISTS idx_messages_parent_id ON messages(parent_id);
