# High-Level Design

## Architecture Overview

The URL Shortener is implemented as a single Flask application with logically separated components.

The application receives requests from the client, validates and processes them through the URL service, and stores URL information in the database.

A cache is used for frequently accessed short URLs to reduce database lookups and improve redirect performance.

## Main Components

### 1. Client

The client sends HTTP requests to create, access, view statistics, or delete short URLs.

### 2. Flask Application

The Flask application acts as the entry point for all API requests and routes requests to the appropriate service.

### 3. URL Service

The URL service contains the main business logic, including:

- URL validation
- Short-code generation
- Custom alias handling
- Expiry checking
- Click counting
- URL deletion

### 4. Cache

The cache stores frequently accessed short-code to long-URL mappings.

For a redirect request, the system first checks the cache. If the URL is not found, it retrieves it from the database and can then store it in the cache.

### 5. SQLite Database

The database provides persistent storage for:

- Short codes
- Original URLs
- Click counts
- Creation time
- Expiry time