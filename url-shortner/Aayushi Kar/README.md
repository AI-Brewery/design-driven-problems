# URL Shortener

## 1. Problem Understanding

The URL Shortener converts a long URL into a short URL that is easier to share.
When a user opens the short URL, they are redirected to the original long URL.
The system also supports optional custom aliases, URL expiry, click tracking, and deletion.

## 2. Functional Requirements

1. The system should create a short URL for a valid long URL.
2. The system should redirect users from a short URL to the original URL.
3. The system should allow users to provide a custom alias.
4. The system should allow users to set an optional expiry time.
5. The system should track the number of successful redirects/clicks.
6. The system should provide statistics for a short URL.
7. The system should allow users to delete a short URL.

## 3. Non-Functional Requirements

1. Redirect requests should be handled with low latency.
2. Every generated short code must be unique.
3. The system should handle a large number of read requests.
4. Expired URLs should not redirect to the original URL.
5. The system should remain reliable when multiple users access URLs simultaneously.
6. The system should validate user input and handle errors gracefully.

## 4. Assumptions

1. The system is publicly accessible and does not require user authentication.
2. Each short code maps to exactly one long URL.
3. A short URL can have an optional expiry time.
4. A successful redirect increments the click count.
5. A deleted or expired short URL cannot be used for redirection.
6. Each custom alias must be unique.

## 5. Capacity Estimation

### Write Requests

The system receives approximately 1 million new URLs per day.

- URLs created per day = 1,000,000
- Seconds per day = 86,400

Average write requests per second:

1,000,000 / 86,400 ≈ 11.6 requests/second

Therefore, the system should handle approximately **12 write requests per second on average**.

### Read Requests

The system receives approximately 100 times more reads than writes.

- Average writes = 12 requests/second
- Average reads = 12 × 100
- Average reads ≈ **1,200 requests/second**

Therefore, the system should handle approximately **1,200 read/redirect requests per second on average**.

### Latency Requirement

Redirect requests should ideally be completed in **less than 50 ms**.

Since reads are much more frequent than writes, caching can be used to reduce database access and improve redirect performance.