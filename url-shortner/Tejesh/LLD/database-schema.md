# Database Schema

## Actual JPA Mapping

Hibernate maps `Url` to the `urls` table. With the development default `spring.jpa.hibernate.ddl-auto=update`, Hibernate creates or updates this schema from the entity. No migration files are present.

| Column | Java field | PostgreSQL shape | Constraints / indexes |
| --- | --- | --- | --- |
| `id` | `Long id` | identity-backed `bigint` | primary key, generated with `IDENTITY` |
| `short_code` | `String shortCode` | variable-length string | `NOT NULL`, unique; the unique constraint is the collision guard |
| `long_url` | `String longUrl` | `varchar(2048)` | `NOT NULL`, maximum length 2048 |
| `created_at` | `Instant createdAt` | timestamp compatible with `Instant` | `NOT NULL`, not updatable after insert |
| `expires_at` | `Instant expiresAt` | timestamp compatible with `Instant` | nullable; indexed by `idx_urls_expires_at` |
| `clicks` | `long clicks` | integer-compatible numeric | `NOT NULL`, initialized by Java to `0` |

The exact vendor-generated timestamp and identity DDL is delegated to Hibernate and the connected PostgreSQL version; this project does not provide a hand-written SQL schema.

## Repository Operations

- `findByShortCode` reads one row.
- `existsByShortCode` checks alias/generated-code availability.
- `findByExpiresAtBefore` selects rows for cleanup using the expiry index.
- `updateClicksByShortCode` performs a parameterized absolute click update inside a modifying transaction.
- JpaRepository supplies save, find, and delete behavior.

## Consistency Notes

`short_code` is the only identity used by the API. The database constraint is authoritative when concurrent requests race. Clicks are persisted asynchronously and can lag the Redis counter. The cleanup query selects rows whose expiry is before the current instant; redirect checks also treat an expiry equal to the current instant as expired.
