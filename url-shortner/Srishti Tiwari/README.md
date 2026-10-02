# URL Shortener

## Functional Requirements

1. Create: the user sends a long URL, and the system returns a short URL for it.
2. Redirect: when the user opens a short link, the system sends them to the original long URL.
3. Custom alias: instead of getting a random code, the user can choose their own short name, but this is optional.
4. Expiry: the user can set a date and time, and after that time the short link stops working. This is optional.
5. Click count: every time someone opens a short link, the system counts the number of clicks, and the user can check this number later.
6. Delete: the user can remove a short link, and after that it no longer works.

## Non-Functional Requirements

1. Write volume: the system must handle 1 million new URLs per day.
2. Read-heavy: for every 1 URL created, about 100 redirects happen, so the redirect path matters most.
3. Speed: a redirect must take under 50 ms.
4. Short codes: codes should be as short as possible.

## Capacity Estimation

Assumption: 1 million new URLs per day, and the system runs for 5 years.

Total URLs: 1,000,000 × 365 × 5 = 1,825,000,000 URLs (about 1.8 billion).

Short code length: a base62 code has 62 possible characters per position (a-z, A-Z, 0-9).
- 5 characters: 62⁵ ≈ 916 million, which is smaller than 1.8 billion, so it's not enough.
- 6 characters: 62⁶ ≈ 56.8 billion, which is bigger than 1.8 billion, so it's enough.

Decision: use 6-character codes, the shortest length that covers the total.

### Storage

One record ≈ 530 bytes (short code 6B + long URL ~500B + created date 8B + expiry date 8B + click count 8B).

Total storage: 530 bytes × 1,825,000,000 records ≈ 967 GB (about 1 TB) over 5 years.

Conclusion: this fits on a single well-provisioned database. No need for a complex distributed storage system from day one.

### QPS (queries per second)

Writes: 1,000,000 ÷ 86,400 seconds ≈ 12 writes per second.

Reads: 12 × 100 ≈ 1,200 reads per second on average, higher at peak traffic.

Conclusion: writes are light. Reads are heavy, and this is why a cache is needed in front of the database, to meet the 50 ms redirect requirement.

## Open Questions

### 1. Short code generation
Decision: counter + base62 encoding.

A single, ever-increasing counter is maintained. Each new URL gets the next number, which is converted into a base62 string to form the short code. This guarantees no two URLs ever get the same code, so there is no need to check for collisions or retry. How counter-to-base62 conversion works:
Each counter value is converted to base62 the same way normal counting rolls over from 9 to 10, except with 62 symbols instead of 10. Example: 65 = 1 group of 62, plus 3 leftover → short code "13".

### 2. Same long URL shortened twice
Decision: always create a new short code, even for a duplicate long URL.

Each short URL tracks its own click count, its own optional expiry, and can be deleted independently. If two requests shared one code, deleting or setting an expiry for one would affect the other, which breaks the per-short-URL tracking the spec requires.

### 3. Custom alias already taken
Decision: reject the request with an error (HTTP 409 Conflict), telling the user the alias is taken.

The system never overwrites an existing alias or silently substitutes a different code, since that could break links already shared by the first user, or confuse the second user about what code they actually got.

### 4. 301 or 302 redirect
Decision: use 302 (temporary) redirect.

A 301 (permanent) redirect gets cached by browsers, which may skip calling the server on repeat visits — breaking click tracking. 302 forces the browser to call the server every time, so every click is counted.

### 5. Preventing code guessing
Decision: scramble the counter number before turning it into a short code.

Since our codes come from a simple counter (1, 2, 3...), someone could guess codes in order and find other people's links. To stop this, the counter is scrambled using a reversible technique (like XOR) before being turned into the final short code. The codes still never repeat, but they no longer look predictable or sequential.

## HLD (High-Level Design)

### Components
- Client: browser/app making requests
- API Server: handles all 4 endpoints
- Cache (Redis): stores recently-used short code → long URL mappings for fast redirects
- Database: permanent storage for all URL records

### Create flow

[Client]
   │
   │  sends long URL (POST /shorten)
   ▼
[API Server]
   │
   │  get next counter value, scramble it, convert to base62
   ▼
[Database]
   │
   │  save new record (shortCode, longUrl, createdAt, expiresAt, clicks=0)
   ▼
[API Server] → returns short URL to [Client]


### Redirect flow

[Client]
   │
   │  clicks sho.rt/aB3xK9
   ▼
[API Server]
   │
   │  "do I have this code cached?"
   ▼
[Cache]
   │
   ├── FOUND  → return long URL to API Server → Client gets redirected (fast, <50ms)
   │
   └── NOT FOUND
          │
          ▼
      [Database]
          │
          │  look up the short code
          ▼
      found long URL
          │
          ├── save it into [Cache] (so next time it's fast)
          │
          ▼
      return long URL to API Server → Client gets redirected

## LLD (Low-Level Design)

### Database Schema

**Table: urls**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT (auto-increment) | internal counter, used to generate short_code |
| short_code | VARCHAR(10) | unique, indexed — either auto-generated or user's custom alias |
| long_url | TEXT | the original long URL |
| created_at | TIMESTAMP | when the link was created |
| expires_at | TIMESTAMP, nullable | optional expiry date/time |
| clicks | INT, default 0 | incremented every time the link is clicked |

**Why `short_code` is indexed:** redirects happen ~1,200 times per second and must respond in under 50ms. An index lets the database jump straight to the matching row instead of scanning every row, like a book's table of contents instead of reading page by page.

**Why `expires_at` is nullable:** expiry is optional, so not every row will have a value here.

### API Specification

**1. Create a short URL**
POST /shorten
Request:  { "longUrl": "https://example.com/very/long/path", "customAlias": "my-blog", "expiresAt": "2027-01-01T00:00:00Z" }
Response: { "shortUrl": "https://sho.rt/my-blog" }

customAlias and expiresAt are optional. If omitted, the system auto-generates a code and the link never expires.

**2. Redirect**
GET /{shortCode}
Response: 302 redirect to the long URL
404 if the code doesn't exist or has expired

**3. Get stats**
GET /stats/{shortCode}
Response: { "longUrl": "...", "clicks": 47, "createdAt": "...", "expiresAt": "..." }

**4. Delete**
DELETE /{shortCode}
Response: 200 OK if deleted
404 if it didn't exist

## How to Run

1. cd into the `src` folder
2. Run `npm install`
3. Run `node server.js`
4. Server runs on http://localhost:3000

## Assumptions
- Service runs for 5 years at 1 million URLs/day (used for all capacity estimates)
- Long URLs average ~500 bytes
- No authentication/user accounts — any client can create or delete any short URL

## Limitations & Future Improvements
- No caching layer (Redis) implemented yet — the HLD includes one, but it was skipped in this implementation due to time constraints. Adding it would involve checking Redis before the database on every redirect.
- No rate limiting — a malicious user could spam `/shorten` or guess short codes rapidly
- SQLite is used for simplicity; a production system would use PostgreSQL/MySQL as sized in the capacity estimation
- No automated tests included — all endpoints were manually verified (see testing commands below)

## Manual Testing Performed
- Created a short URL, redirected successfully, verified click count incremented, deleted it
- Verified invalid URL is rejected (400)
- Verified duplicate custom alias is rejected (409)

