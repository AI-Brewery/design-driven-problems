package com.tejesh.urlshortener.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisServiceImpl implements RedisService {

    private static final String URL_PREFIX = "url:";
    private static final String CLICKS_PREFIX = "clicks:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RedisServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<CachedUrl> getUrl(String shortCode) {
        try {
            String value = redisTemplate.opsForValue().get(urlKey(shortCode));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, CachedUrl.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    @Override
    public void cacheUrl(String shortCode, CachedUrl url, Duration ttl) {
        try {
            if (!ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(urlKey(shortCode), objectMapper.writeValueAsString(url), ttl);
            }
        } catch (JsonProcessingException | RuntimeException ignored) {
            // Redis is an optimization; PostgreSQL remains authoritative.
        }
    }

    @Override
    public Long incrementClicks(String shortCode) {
        try {
            return redisTemplate.opsForValue().increment(clicksKey(shortCode));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public Long getClicks(String shortCode) {
        try {
            String value = redisTemplate.opsForValue().get(clicksKey(shortCode));
            return value == null ? null : Long.valueOf(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public void initializeClicks(String shortCode, long clicks) {
        try {
            redisTemplate.opsForValue().setIfAbsent(clicksKey(shortCode), Long.toString(clicks));
        } catch (RuntimeException ignored) {
            // A later stats request can retry initialization.
        }
    }

    @Override
    public void invalidate(String shortCode) {
        try {
            redisTemplate.delete(urlKey(shortCode));
            redisTemplate.delete(clicksKey(shortCode));
        } catch (RuntimeException ignored) {
            // The database delete has already completed; cache invalidation is best effort.
        }
    }

    private String urlKey(String shortCode) {
        return URL_PREFIX + shortCode;
    }

    private String clicksKey(String shortCode) {
        return CLICKS_PREFIX + shortCode;
    }
}