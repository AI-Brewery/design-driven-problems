# URL Shortener

> **Assigned:** Wednesday, 16 September 2026  
> **Deadline:** Thursday, 24 September 2026 — Pull Request must be opened by then

## Context

Long URLs are hard to share. Build a service like bit.ly that turns a long URL into a short one and redirects anyone who opens the short link to the original URL.

```text
https://example.com/articles/2026/system-design/how-to-build-scalable-apps?ref=home
        ↓
https://sho.rt/aB3xK9
```

---

## What the System Must Do

- Create a short URL for a given long URL
- Redirect a short URL to the original long URL
- Support an optional **custom alias** (e.g. `sho.rt/my-blog`)
- Support an optional **expiry time**, after which the link stops working
- Track basic **click count** per short URL
- Delete a short URL

---

## API

```text
POST   /shorten          { longUrl, customAlias?, expiresAt? }  → { shortUrl }
GET    /{shortCode}      → 302 redirect to longUrl (404 if missing/expired)
GET    /stats/{shortCode} → { longUrl, clicks, createdAt, expiresAt }
DELETE /{shortCode}
```

---

## Scale Assumptions

- 1 million new URLs per day
- Reads are ~100× more than writes
- Redirects should be fast (< 50 ms)
- Short codes should be as short as possible

Use these numbers to estimate storage and justify your design.

---

## Open Questions — You Must Decide

1. How do you generate the short code? (hashing, counter + base62, random?) What happens on collision?
2. Same long URL shortened twice — same short code or a new one?
3. Custom alias is already taken — what do you do?
4. 301 or 302 redirect? How does it affect click tracking?
5. How do you stop someone from guessing or enumerating short codes?

Write your choice and why.

---

## Submission Checklist

- [ ] Functional & non-functional requirements
- [ ] Capacity estimation (storage, QPS)
- [ ] HLD: architecture diagram (DB, cache, services)
- [ ] LLD: DB schema, classes, short-code generation logic
- [ ] Working implementation of all APIs
- [ ] Open questions answered
- [ ] Edge cases handled (invalid URL, expired link, duplicate alias)
