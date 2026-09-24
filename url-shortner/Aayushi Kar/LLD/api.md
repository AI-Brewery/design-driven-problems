# API Design

The URL Shortener exposes REST APIs for creating, accessing, viewing statistics, and deleting shortened URLs.

## 1. Create Short URL

### Endpoint

`POST /shorten`

### Request Body

```json
{
  "longUrl": "https://example.com/some/long/url",
  "customAlias": "my-link",
  "expiresAt": "2026-12-31T23:59:59"
}
```

`customAlias` and `expiresAt` are optional.

### Success Response

**HTTP 201 Created**

```json
{
  "shortUrl": "http://localhost:5000/abc123"
}
```

### Possible Errors

* `400 Bad Request` — Invalid or missing URL.
* `400 Bad Request` — Invalid expiry time.
* `409 Conflict` — Custom alias already exists.

---

## 2. Redirect to Original URL

### Endpoint

`GET /<shortCode>`

### Example

`GET /abc123`

### Success

**HTTP 302 Found**

The server redirects the user to the original long URL.

The click count is incremented after a successful lookup.

### Possible Errors

* `404 Not Found` — Short code does not exist.
* `404 Not Found` — URL has expired.

---

## 3. Get URL Statistics

### Endpoint

`GET /stats/<shortCode>`

### Example

`GET /stats/abc123`

### Success Response

**HTTP 200 OK**

```json
{
  "longUrl": "https://example.com/some/long/url",
  "clicks": 15,
  "createdAt": "2026-09-24T18:30:00",
  "expiresAt": null
}
```

### Possible Errors

* `404 Not Found` — Short code does not exist.

---

## 4. Delete Short URL

### Endpoint

`DELETE /<shortCode>`

### Success Response

**HTTP 204 No Content**

The shortened URL is deleted and can no longer be used for redirection.

### Possible Errors

* `404 Not Found` — Short code does not exist.

---

## API Summary

| Method | Endpoint             | Purpose                  |
| ------ | -------------------- | ------------------------ |
| POST   | `/shorten`           | Create a short URL       |
| GET    | `/<shortCode>`       | Redirect to original URL |
| GET    | `/stats/<shortCode>` | View URL statistics      |
| DELETE | `/<shortCode>`       | Delete a short URL       |
