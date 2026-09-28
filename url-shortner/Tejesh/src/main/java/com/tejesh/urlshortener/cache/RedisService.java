package com.tejesh.urlshortener.cache;

import java.time.Duration;
import java.util.Optional;

public interface RedisService {

    Optional<CachedUrl> getUrl(String shortCode);

    void cacheUrl(String shortCode, CachedUrl url, Duration ttl);

    Long incrementClicks(String shortCode);

    Long getClicks(String shortCode);

    void initializeClicks(String shortCode, long clicks);

    void invalidate(String shortCode);
}