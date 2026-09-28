# API Design

## Create Short URL

`POST /shorten`

Request:

```json
{
  "longUrl": "https://example.com/article",
  "customAlias": "article-2026",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

`customAlias` and `expiresAt` are optional. `longUrl` must be a valid HTTP or HTTPS URL with a host. The alias must match `[A-Za-z0-9_-]{3,64}` and expiry must be in the future.

Success: `201 Created`

```json
{"shortUrl":"http://localhost:8080/article-2026"}
```

Without an alias, `shortCode` is seven random Base62 characters.

## Redirect

`GET /{shortCode}`

Success: `302 Found` with `Location: https://example.com/article`. The response body is empty. Redis is checked first; PostgreSQL is used on a miss or Redis failure. A successful redirect increments the Redis counter best effort.

## Statistics

`GET /stats/{shortCode}`

Success: `200 OK`

```json
{
  "longUrl":"https://example.com/article",
  "clicks":3,
  "createdAt":"2026-09-28T12:00:00Z",
  "expiresAt":null
}
```

Metadata comes from PostgreSQL. `clicks` comes from Redis when available, otherwise the persisted PostgreSQL value; a missing Redis counter is initialized from PostgreSQL.

## Delete

`DELETE /{shortCode}`

Success: `204 No Content`. PostgreSQL is deleted first, followed by best-effort removal of `url:{shortCode}` and `clicks:{shortCode}`.

## Error Responses

```json
{
  "timestamp":"2026-09-28T12:00:00Z",
  "status":404,
  "error":"URL_NOT_FOUND",
  "message":"Short URL not found: missing"
}
```

| Condition | Status | Error code |
| --- | --- | --- |
| Invalid JSON, validation, URL, alias, or expiry | 400 | `INVALID_REQUEST` |
| Missing URL or expired redirect | 404 | `URL_NOT_FOUND` or `URL_EXPIRED` |
| Existing custom alias | 409 | `ALIAS_ALREADY_EXISTS` |
| Unhandled database integrity conflict | 409 | `SHORT_CODE_CONFLICT` |
| Unexpected failure | 500 | `INTERNAL_ERROR` |

The exact exception message is implementation-generated; clients should use the status and error code.
