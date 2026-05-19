#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.profiling.yml"

TOP_LEVEL_COUNT="${TOP_LEVEL_COUNT:-5000}"
PARENT_SAMPLE_COUNT="${PARENT_SAMPLE_COUNT:-300}"
REPLIES_PER_PARENT="${REPLIES_PER_PARENT:-10}"
HOT_DIRECT_REPLIES="${HOT_DIRECT_REPLIES:-1000}"
HOT_CHAIN_DEPTH="${HOT_CHAIN_DEPTH:-50}"
HOT_REACTIONS="${HOT_REACTIONS:-500}"
RANDOM_REACTIONS="${RANDOM_REACTIONS:-10}"

POSTGRES_DB="${POSTGRES_DB:-beforum_profiling}"
POSTGRES_USER="${POSTGRES_USER:-beforum}"

printf "[profiling] starting isolated stack...\n"
docker compose -f "$COMPOSE_FILE" up -d db

DB_CONTAINER="$(docker compose -f "$COMPOSE_FILE" ps -q db)"
if [ -z "$DB_CONTAINER" ]; then
  echo "[profiling] failed to resolve db container id"
  exit 1
fi

printf "[profiling] waiting for db health...\n"
until [ "$(docker inspect -f '{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null || true)" = "healthy" ]; do
  sleep 2
done

printf "[profiling] seeding heavy dataset...\n"
docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" \
  -v top_level_count="$TOP_LEVEL_COUNT" \
  -v parent_sample_count="$PARENT_SAMPLE_COUNT" \
  -v replies_per_parent="$REPLIES_PER_PARENT" \
  -v hot_direct_replies="$HOT_DIRECT_REPLIES" \
  -v hot_chain_depth="$HOT_CHAIN_DEPTH" \
  -v hot_reactions="$HOT_REACTIONS" \
  -v random_reactions="$RANDOM_REACTIONS" \
  -f - < "$SCRIPT_DIR/seed_profiling.sql"

printf "[profiling] done. app (if started) is on http://localhost:${PROFILING_APP_PORT:-18080}\n"
printf "[profiling] db is on localhost:${PROFILING_DB_PORT:-55432}\n"
