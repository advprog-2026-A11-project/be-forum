# Profiling Dataset

This folder provides an isolated environment and seed script to expose backend performance bottlenecks without touching your default DB from `docker-compose.yml` / `docker-compose.dev.yml`.

## Files
- `docker-compose.profiling.yml`: separate PostgreSQL volume + separate app/db ports.
- `seed_profiling.sql`: bulk SQL inserts designed to stress known hotspots.
- `seed_profiling_data.sh`: runner script to bring up profiling DB and execute seeding.
- `reset_profiling_db.sh`: reset profiling DB schema (`DROP SCHEMA public CASCADE` + recreate).

## Start isolated profiling stack
```bash
docker compose -f profiling/docker-compose.profiling.yml up --build -d
```

Default ports:
- App: `18080`
- DB: `55432`

## Reset DB
```bash
bash profiling/reset_profiling_db.sh
```

After reset:
1. Run backend once so Flyway migrations recreate tables.
2. Seed again with `bash profiling/seed_profiling_data.sh`.

## Seed heavy data
```bash
bash profiling/seed_profiling_data.sh
```

## Optional size tuning
```bash
TOP_LEVEL_COUNT=80000 \
PARENT_SAMPLE_COUNT=5000 \
REPLIES_PER_PARENT=30 \
HOT_DIRECT_REPLIES=10000 \
HOT_REACTIONS=5000 \
RANDOM_REACTIONS=60 \
bash profiling/seed_profiling_data.sh
```

## Why this data reveals the 5 issues
- Large top-level corpus => expensive unbounded `GET /api/messages`.
- Replies for many parents + recursive mapping => potential N+1 and heavy serialization.
- One giant hot thread => costly `GET /api/messages/{id}`.
- Many reactions per hot message => expensive repeated count queries per reaction type.
- High overall rows with filter/sort patterns => index and query-plan weaknesses become visible.
