package com.tejesh.urlshortener.cache;

import java.time.Instant;

public record CachedUrl(String longUrl, Instant expiresAt) {
}