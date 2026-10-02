# Capacity Estimation

These are assignment planning estimates, not measurements of this application.

## Traffic

Assume 1,000,000 new URLs per day:

- Average write QPS: `1,000,000 / 86,400 = 11.57`, approximately **12 QPS**.
- With a 100:1 read-to-write ratio, average read QPS: `12 * 100 = 1,200 QPS`.
- A reasonable planning peak is 5x average: approximately **60 write QPS** and **6,000 read QPS**.

The code does not include a load test or benchmark, so these rates and any latency target remain design assumptions.

## Storage

Using the assignment estimate of approximately 1 KB per URL row:

- Daily logical URL storage: approximately **1 GB/day**.
- Yearly logical URL storage: approximately **365 GB/year**.

This excludes PostgreSQL indexes, page overhead, WAL, replication, backups, Redis memory, and operational metadata. The actual `long_url` column permits up to 2,048 characters, so real storage varies by data.

## Short-Code Namespace

`ShortCodeGenerator` uses seven independent characters from a 62-character Base62 alphabet:

`62^7 = 3,521,614,606,208` possible generated values.

The generator uses `SecureRandom`; custom aliases share the same database column and uniqueness constraint. The service retries generated codes up to ten times after a collision. Namespace size alone does not eliminate collision probability, which is why the database constraint remains authoritative.

## Capacity Implications

Redis is intended to absorb repeated redirect reads and hold live counters. PostgreSQL handles creation, cache misses, statistics metadata, deletion, cleanup, and periodic click persistence. The current background click job scans all URL rows, so its cost grows with the number of stored URLs and is a known scaling limitation.
