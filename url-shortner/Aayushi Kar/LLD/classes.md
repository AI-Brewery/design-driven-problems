# Class Design

The URL Shortener is divided into small logical components so that URL creation, short-code generation, and database operations are handled separately.

## 1. URL

Represents a shortened URL stored in the system.

### Attributes

* `id` — Unique database ID
* `short_code` — Generated or custom short code
* `long_url` — Original URL
* `clicks` — Number of successful redirects
* `created_at` — Creation timestamp
* `expires_at` — Optional expiry timestamp

---

## 2. ShortCodeGenerator

Responsible for generating random short codes.

### Methods

* `generate()` — Generates a random alphanumeric short code.

### Design Decision

A random 6-character alphanumeric code is used.

There are:

`62^6 ≈ 56.8 billion`

possible combinations using uppercase letters, lowercase letters, and digits.

If a generated code already exists, another code is generated.

---

## 3. URLRepository

Responsible for communication with the SQLite database.

### Methods

* `create_url()` — Stores a new shortened URL.
* `get_by_code()` — Finds a URL using its short code.
* `increment_clicks()` — Increases the click count.
* `delete_by_code()` — Deletes a shortened URL.
* `get_stats()` — Retrieves statistics for a shortened URL.

---

## 4. URLService

Contains the main business logic of the application.

### Methods

* `create_short_url()` — Validates the request, handles custom aliases, generates a code, and stores the URL.
* `get_url()` — Retrieves the original URL and checks expiry.
* `record_click()` — Updates the click count.
* `get_stats()` — Returns statistics.
* `delete_url()` — Deletes a shortened URL.

---

## 5. Cache

Stores frequently accessed short-code mappings temporarily to reduce database reads.

### Operations

* `get()` — Retrieve a URL from cache.
* `set()` — Store a URL in cache.
* `delete()` — Remove a URL from cache.

### Implementation

For the student implementation, an in-memory Python dictionary will be used.

In a production system, this could be replaced with Redis.

---

## 6. Flask API Layer

Handles HTTP requests and responses.

### Endpoints

* `POST /shorten`
* `GET /<shortCode>`
* `GET /stats/<shortCode>`
* `DELETE /<shortCode>`

The API layer passes the request to `URLService` and returns the appropriate HTTP response.

---

## Component Interaction

```text
Flask API Layer
       |
       v
   URLService
    /      \
   v        v
Cache    URLRepository
              |
              v
          SQLite DB
       ^
       |
ShortCodeGenerator
```

The API layer handles HTTP communication, while `URLService` handles business logic. `URLRepository` handles database operations, and `ShortCodeGenerator` handles short-code creation.
