# URL Shortener

## Project Overview

This project is a Spring Boot URL shortener. It stores URL metadata in PostgreSQL, uses Redis for the redirect cache and click counters, and exposes REST endpoints for creation, redirection, statistics, and deletion.

## Problem Statement Summary

Create short URLs for HTTP/HTTPS destinations, optionally accept a caller-provided alias and expiry time, redirect short codes, count clicks, expose statistics, and delete links.

## Functional Requirements

- `POST /shorten` creates a generated short code or a custom alias.
- `GET /{shortCode}` returns an HTTP `302 Found` redirect and records a click when possible.
- `GET /stats/{shortCode}` returns destination metadata and the current click count.
- `DELETE /{shortCode}` deletes the PostgreSQL row and best-effort Redis entries.
- Validate HTTP/HTTPS destinations, aliases, and future expiry timestamps.
- Return `404` for missing or expired redirect targets and `409` for alias conflicts.

## Non-Functional Requirements

- PostgreSQL is the authoritative store for URL metadata.
- Redis is an optimization and must not be required for redirect correctness.
- Redirects avoid synchronous PostgreSQL click updates.
- Click persistence is eventually consistent and retried by scheduled work.
- Database access uses a bounded HikariCP pool; configuration is environment-driven.
- No latency benchmark is included, so a sub-50 ms redirect target is not proven.

## Assumptions and Constraints

- Capacity planning uses 1,000,000 new URLs/day and a 100:1 read/write ratio.
- One URL row is estimated at approximately 1 KB for planning only.
- Generated codes are seven characters from a 62-character alphabet.
- No authentication, authorization, rate limiter, load balancer, queue, or microservice split is implemented.
- JPA `ddl-auto=update` is a development default; production schema migration is outside this implementation.

## Technology Stack

Java 25, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Hibernate, PostgreSQL, Redis via `StringRedisTemplate`, HikariCP, Jakarta Validation, Jackson, Lombok, Maven, and JUnit/Mockito/Spring test support.

## Architecture Summary

`UrlController` delegates to `UrlService`. `UrlService` reads and writes `UrlRepository`, reads through Redis on redirects, and updates Redis counters. `UrlBackgroundJobs` periodically copies Redis counters to PostgreSQL and removes expired rows. See [HLD/architecture.md](HLD/architecture.md) and [HLD/architecture-diagram.md](HLD/architecture-diagram.md).

## API Summary

| Method | Path | Success |
| --- | --- | --- |
| POST | `/shorten` | `201 Created`, JSON short URL |
| GET | `/{shortCode}` | `302 Found`, `Location` header |
| GET | `/stats/{shortCode}` | `200 OK`, JSON statistics |
| DELETE | `/{shortCode}` | `204 No Content` |

Full contracts and examples are in [LLD/api-design.md](LLD/api-design.md).

## Setup Instructions

Prerequisites: JDK 25, Maven 3.9+, PostgreSQL, and Redis. Create the database, for example:

```sql
CREATE DATABASE urlshortener;
```

Copy `.env.example` to `.env` and provide `DB_PASSWORD`. The default connections are PostgreSQL at `localhost:5432/urlshortener` and Redis at `localhost:6379`. The application can use environment variables listed in `src/main/resources/application.yml`, including `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_URL`, `SERVER_PORT`, `REDIS_CACHE_TTL`, and scheduled-job delays.

## How to Run the Application

```powershell
mvn spring-boot:run
```

The default HTTP port is `8080`. The response base URL defaults to `http://localhost:8080` and can be overridden with the `url-shortener.base-url` property.

## How to Run Tests

```powershell
mvn test
```

The context and integration tests require a reachable PostgreSQL instance with matching credentials. Unit tests cover service, controller, click-counting, collision, expiry, and scheduled-job behavior.

## Edge Cases Handled

- Blank, malformed, non-HTTP, and non-HTTPS destination URLs.
- Invalid aliases, duplicate aliases, and generated-code database collisions.
- Expiry at creation, cache-hit expiry, cache-miss expiry, and scheduled cleanup.
- Missing short codes, Redis failures, missing Redis counters, and deleted links.
- Click persistence failures are isolated per record and retried on a later run.
- Unexpected failures return a generic JSON `500` response.

## Limitations

- Redis invalidation after deletion is best effort; stale cached data can remain until its TTL if Redis is unavailable during deletion.
- Click statistics can lag until the scheduled persistence job runs.
- Statistics query PostgreSQL for metadata but do not independently reject an expired row; redirect expiry is enforced and cleanup is scheduled.
- No duplicate-long-URL deduplication, authentication, rate limiting, distributed deployment, or schema migration tooling is included.
- No benchmark or production availability measurement is included.

## Future Improvements

Use versioned migrations and `ddl-auto=validate`, add authentication and edge rate limiting, use highly available Redis/PostgreSQL, add metrics/tracing and benchmark tests, define retention for click counters, and make deletion revocation immediate with a durable tombstone/version check.

## Design vs Implementation Consistency

The design artifacts in this submission describe the implementation currently present: a single Spring Boot application, PostgreSQL source of truth, Redis read-through cache and counter store, database-backed uniqueness, scheduled click persistence, and scheduled expiry cleanup. The implementation does not contain the commonly proposed but absent load balancer, queue, microservices, authentication, or rate limiter. Capacity figures are planning assumptions, not measurements. The seven-character code length and `62^7` namespace match `ShortCodeGenerator`.

## Additional Configuration and Capacity Notes

The default JPA mode is `update`, SQL logging is enabled, and the Hikari pool defaults to 20 maximum and 5 minimum idle connections. Redis URL entries use a one-hour default TTL or the remaining expiry duration, whichever is applicable. Click keys are not assigned a TTL by this implementation so the scheduled worker can persist them.

The capacity arithmetic is documented in [HLD/capacity-estimation.md](HLD/capacity-estimation.md); it does not claim that the application has been benchmarked at those rates.
