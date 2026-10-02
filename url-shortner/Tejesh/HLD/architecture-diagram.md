# Architecture Diagrams

## Component Diagram

```mermaid
flowchart TB
    Client[HTTP client]
    Controller[UrlController]
    Service[UrlService]
    Repository[UrlRepository]
    Entity[Url entity]
    PostgreSQL[(PostgreSQL urls table)]
    RedisService[RedisService / RedisServiceImpl]
    Redis[(Redis)]
    Jobs[UrlBackgroundJobs]

    Client --> Controller
    Controller --> Service
    Service --> Repository
    Repository --> Entity
    Entity --> PostgreSQL
    Service --> RedisService
    RedisService --> Redis
    Jobs --> Repository
    Jobs --> RedisService
```

## Redirect Decision Diagram

```mermaid
flowchart TD
    Start[GET /{shortCode}] --> Cache{Redis URL present?}
    Cache -->|yes| CacheExpiry{Cached expiry passed?}
    CacheExpiry -->|yes| Invalidate[Invalidate URL key]
    Invalidate --> Expired404[404 URL_EXPIRED]
    CacheExpiry -->|no| CountHit[Increment Redis clicks]
    CountHit --> Redirect[302 Location: longUrl]
    Cache -->|no or Redis failure| DB{PostgreSQL row present?}
    DB -->|no| Missing404[404 URL_NOT_FOUND]
    DB -->|yes| DBExpiry{Database expiry passed?}
    DBExpiry -->|yes| ExpiredDB404[404 URL_EXPIRED]
    DBExpiry -->|no| Populate[Cache URL with TTL]
    Populate --> CountMiss[Increment Redis clicks]
    CountMiss --> Redirect
```

## Scheduled Work

```mermaid
flowchart LR
    Scheduler[Spring @Scheduled] --> ClickJob[Persist click counts]
    ClickJob --> Redis[(clicks:shortCode)]
    ClickJob --> Update[Absolute UPDATE by short code]
    Update --> PostgreSQL[(PostgreSQL)]
    Scheduler --> ExpiryJob[Delete expired URLs]
    ExpiryJob --> PostgreSQL
```
