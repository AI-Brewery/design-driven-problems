# Design Decisions

## PostgreSQL as Authority

URL metadata and the last persisted click count live in PostgreSQL. Redis improves the redirect path but failures fall back to PostgreSQL for URL lookup.

## Random Seven-Character Codes

`ShortCodeGenerator` uses `SecureRandom` and seven characters selected independently from Base62. The namespace is `62^7 = 3,521,614,606,208`. The service attempts generated codes at most ten times. A database unique constraint protects against races between the existence check and insert.

## Custom Aliases

Aliases share `short_code` with generated values, are validated to 3-64 `[A-Za-z0-9_-]` characters, and return `409 ALIAS_ALREADY_EXISTS` when already present or when the insert loses a uniqueness race.

## No Duplicate Destination Reuse

The implementation does not look up an existing `longUrl` before creating a row. Repeated requests for the same destination receive separate short codes unless the caller supplies an existing alias.

## Redirect Status and Counting

Redirects use `302 Found`, not `301`, because the service performs click counting and retains the ability to change destinations in a future implementation. Counting is a Redis increment after the URL is accepted; it is not a synchronous PostgreSQL update.

## Expiry

Input validation accepts only future expiries. Redirects enforce expiry on both Redis hits and PostgreSQL misses. Redis TTL is bounded by the expiry. A fixed-delay scheduled job deletes expired PostgreSQL rows. Statistics do not call the service expiry validator, so their behavior differs from redirects until cleanup runs.

## Click Persistence

The scheduled job copies the Redis counter as an absolute value. This makes repeated job runs idempotent and avoids double-counting. It updates by `short_code`, so a deleted row is harmless. Redis counters are not assigned a TTL because they are needed by the persistence job.

## Delete and Cache Invalidation

Deletion removes PostgreSQL first, then invalidates both Redis keys. Invalidation is best effort. If Redis is unavailable, a previously cached redirect can remain available until its TTL, which is documented as a limitation.

## Failure and Security Choices

Redis errors do not fail redirects, cache population, counter increments, or deletion when the database operation can proceed. Unexpected API errors are generic. URL validation restricts destinations to HTTP/HTTPS with a host, while no authentication or rate limiting is implemented.

## Design vs Implementation Consistency

The current HLD/LLD describe the code that exists: one Spring Boot application, PostgreSQL, Redis, scheduled jobs, and synchronous REST calls. No unimplemented queue, load balancer, authentication service, or rate limiter is included in the diagrams. Capacity values are assumptions and have not been benchmarked. The code and design agree on seven-character Base62 generation, Redis-first redirects, eventual click persistence, and best-effort cache invalidation.
