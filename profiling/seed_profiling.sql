-- Seed profile-heavy dataset to expose scaling bottlenecks.
-- Variables expected from psql -v:
-- top_level_count, parent_sample_count, replies_per_parent,
-- hot_direct_replies, hot_chain_depth, hot_reactions, random_reactions
-- All generated messages use one reading_id:
-- aeccef9d-31b1-4018-a745-dbfb5c6c27d0

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

INSERT INTO messages (id, content, created_at, reading_id, user_id, parent_id, version)
SELECT
  gen_random_uuid(),
  '[profiling][top] message #' || gs::text,
  NOW() - (gs || ' seconds')::interval,
  'aeccef9d-31b1-4018-a745-dbfb5c6c27d0',
  gen_random_uuid(),
  NULL,
  0
FROM generate_series(1, :'top_level_count'::int) AS gs;

WITH new_root AS (
  INSERT INTO messages (id, content, created_at, reading_id, user_id, parent_id, version)
  VALUES (
    gen_random_uuid(),
    '[profiling][hot-root] massive-thread-root',
    NOW(),
    'aeccef9d-31b1-4018-a745-dbfb5c6c27d0',
    gen_random_uuid(),
    NULL,
    0
  )
  RETURNING id
)
SELECT id FROM new_root;

CREATE TEMP TABLE profiling_parents AS
SELECT id
FROM messages
WHERE parent_id IS NULL
  AND content LIKE '[profiling][top] %'
ORDER BY created_at DESC
LIMIT :'parent_sample_count'::int;

INSERT INTO messages (id, content, created_at, reading_id, user_id, parent_id, version)
SELECT
  gen_random_uuid(),
  '[profiling][reply] parent=' || p.id::text || ' reply#' || r.gs::text,
  NOW() - ((r.gs % 300) || ' seconds')::interval,
  'aeccef9d-31b1-4018-a745-dbfb5c6c27d0',
  gen_random_uuid(),
  p.id,
  0
FROM profiling_parents p
CROSS JOIN LATERAL generate_series(1, :'replies_per_parent'::int) AS r(gs);

CREATE TEMP TABLE profiling_hot_root AS
SELECT id
FROM messages
WHERE content = '[profiling][hot-root] massive-thread-root'
ORDER BY created_at DESC
LIMIT 1;

INSERT INTO messages (id, content, created_at, reading_id, user_id, parent_id, version)
SELECT
  gen_random_uuid(),
  '[profiling][hot-reply] #' || gs::text,
  NOW() - (gs || ' milliseconds')::interval,
  'aeccef9d-31b1-4018-a745-dbfb5c6c27d0',
  gen_random_uuid(),
  (SELECT id FROM profiling_hot_root LIMIT 1),
  0
FROM generate_series(1, :'hot_direct_replies'::int) AS gs;

SELECT set_config('profiling.hot_chain_depth', :'hot_chain_depth', false);

DO $$
DECLARE
  i int;
  max_depth int;
  current_parent uuid;
  next_id uuid;
BEGIN
  max_depth := current_setting('profiling.hot_chain_depth')::int;
  SELECT id INTO current_parent FROM profiling_hot_root LIMIT 1;

  FOR i IN 1..max_depth LOOP
    next_id := gen_random_uuid();
    INSERT INTO messages (id, content, created_at, reading_id, user_id, parent_id, version)
    VALUES (
      next_id,
      '[profiling][hot-chain] depth=' || i::text,
      NOW() - (i || ' milliseconds')::interval,
      'aeccef9d-31b1-4018-a745-dbfb5c6c27d0',
      gen_random_uuid(),
      current_parent,
      0
    );
    current_parent := next_id;
  END LOOP;
END $$;

CREATE TEMP TABLE profiling_hot_users AS
SELECT gen_random_uuid() AS user_id
FROM generate_series(1, :'hot_reactions'::int);

INSERT INTO reactions (id, reaction_type, user_id, message_id, created_at)
SELECT
  gen_random_uuid(),
  t.reaction_type,
  u.user_id,
  (SELECT id FROM profiling_hot_root LIMIT 1),
  NOW() - ((row_number() OVER()) || ' milliseconds')::interval
FROM profiling_hot_users u
CROSS JOIN (VALUES
  ('UPVOTE'), ('DOWNVOTE'), ('FIRE'), ('ROCKET'), ('LAUGH'), ('PARTY'), ('THINKING')
) AS t(reaction_type);

CREATE TEMP TABLE profiling_message_pool AS
SELECT id
FROM messages
WHERE content LIKE '[profiling][top] %'
ORDER BY random()
LIMIT GREATEST(1000, :'parent_sample_count'::int * 2);

INSERT INTO reactions (id, reaction_type, user_id, message_id, created_at)
SELECT
  gen_random_uuid(),
  (ARRAY['UPVOTE','DOWNVOTE','FIRE','ROCKET','LAUGH','PARTY','THINKING'])[1 + floor(random() * 7)::int],
  gen_random_uuid(),
  mp.id,
  NOW() - ((gs % 10000) || ' milliseconds')::interval
FROM profiling_message_pool mp
CROSS JOIN LATERAL generate_series(1, :'random_reactions'::int) AS g(gs);

COMMIT;
