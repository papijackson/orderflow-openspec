# Flyway Integration

Flyway is the source of truth for production schema evolution.

## Rules

- Versioned migrations are immutable after application to a shared environment.
- Never manually change production schema.
- Never use Hibernate `ddl-auto=update/create/create-drop` in production.
- Test migrations against PostgreSQL.
- Test representative existing data for non-trivial migrations.
- Use expand/contract for rolling deployments where compatibility requires it.
- Treat destructive migrations as high-risk changes requiring explicit review.
- For large indexes, evaluate `CREATE INDEX CONCURRENTLY` and Flyway transaction behavior.

## CI

A migration gate should prove that:
1. migrations run;
2. schema is valid;
3. application starts against the migrated schema;
4. representative data remains compatible.
