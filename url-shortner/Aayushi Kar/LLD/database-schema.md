# Database Schema

## 1. URLs Table

The `urls` table stores the mapping between a short code and its original long URL.

| Column     | Data Type   | Constraints                 | Description                          |
| ---------- | ----------- | --------------------------- | ------------------------------------ |
| id         | INTEGER     | Primary Key, Auto Increment | Unique identifier for each URL       |
| short_code | VARCHAR(10) | UNIQUE, NOT NULL            | Short code used in the shortened URL |
| long_url   | TEXT        | NOT NULL                    | Original URL                         |
| clicks     | INTEGER     | NOT NULL, DEFAULT 0         | Number of successful redirects       |
| created_at | DATETIME    | NOT NULL                    | Time when the short URL was created  |
| expires_at | DATETIME    | NULL                        | Optional expiry time                 |

## 2. Relationships

The system currently uses a single table, so there are no foreign-key relationships.

Each `short_code` uniquely identifies one stored URL.

## 3. Indexing

A unique index is created on `short_code` to allow fast lookup during redirects.

This is important because redirect requests are expected to be much more frequent than URL creation requests.

## 4. Example Record

| id | short_code | long_url                      | clicks | created_at          | expires_at |
| -: | ---------- | ----------------------------- | -----: | ------------------- | ---------- |
|  1 | aB7xK2     | https://example.com/long-page |     12 | 2026-09-24 18:30:00 | NULL       |

## 5. Database Operations

The application needs to support the following operations:

* Insert a new shortened URL.
* Find a URL using its `short_code`.
* Update the click count after a successful redirect.
* Retrieve URL statistics.
* Delete a shortened URL.
* Check whether a URL has expired.
