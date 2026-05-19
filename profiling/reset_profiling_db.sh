#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.profiling.yml"

POSTGRES_DB="${POSTGRES_DB:-beforum_profiling}"
POSTGRES_USER="${POSTGRES_USER:-beforum}"

printf "[profiling] ensuring profiling db container is running...\n"
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

printf "[profiling] resetting schema in database '%s'...\n" "$POSTGRES_DB"
docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" \
  -c "DROP SCHEMA IF EXISTS public CASCADE;" \
  -c "CREATE SCHEMA public;" \
  -c "GRANT ALL ON SCHEMA public TO ${POSTGRES_USER};" \
  -c "GRANT ALL ON SCHEMA public TO public;"

printf "[profiling] reset complete.\n"
printf "[profiling] next: run the app once so Flyway migrations recreate tables, then run seed script.\n"
