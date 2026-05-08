-- First, create a temporary column with UUID type
ALTER TABLE reactions ADD COLUMN user_id_new UUID;

-- Copy and convert data from old column to new column
UPDATE reactions SET user_id_new = user_id::uuid WHERE user_id IS NOT NULL;

-- Drop the old column
ALTER TABLE reactions DROP COLUMN user_id;

-- Rename the new column to the original name
ALTER TABLE reactions RENAME COLUMN user_id_new TO user_id;

-- Add NOT NULL constraint
ALTER TABLE reactions ALTER COLUMN user_id SET NOT NULL;
