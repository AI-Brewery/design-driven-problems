package com.tejesh.urlshortener.service;

import com.tejesh.urlshortener.cache.CachedUrl;
import com.tejesh.urlshortener.cache.RedisService;
import com.tejesh.urlshortener.dto.ShortenRequest;
import com.tejesh.urlshortener.dto.ShortenResponse;
import com.tejesh.urlshortener.dto.StatsResponse;
import com.tejesh.urlshortener.entity.Url;
import com.tejesh.urlshortener.exception.AliasAlreadyExistsException;
import com.tejesh.urlshortener.exception.ExpiredUrlException;
import com.tejesh.urlshortener.exception.UrlNotFoundException;
import com.tejesh.urlshortener.repository.UrlRepository;
import com.tejesh.urlshortener.util.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UrlService {

    private static final int MAX_GENERATED_CODE_ATTEMPTS = 10;
    private static final String ALIAS_PATTERN = "^[A-Za-z0-9_-]{3,64}$";

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisService redisService;
    private final String baseUrl;
    private final Duration cacheTtl;

    public UrlService(UrlRepository urlRepository, ShortCodeGenerator shortCodeGenerator) {
        this(urlRepository, shortCodeGenerator, new NoOpRedisService(), "http://localhost:8080", Duration.ofHours(1));
    }

    @Autowired
    public UrlService(
            UrlRepository urlRepository,
            ShortCodeGenerator shortCodeGenerator,
            RedisService redisService,
            @Value("${url-shortener.base-url:http://localhost:8080}") String baseUrl,
            @Value("${url-shortener.cache.ttl:PT1H}") Duration cacheTtl) {
        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.redisService = redisService;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.cacheTtl = cacheTtl;
    }

    public ShortenResponse shorten(ShortenRequest request) {
        validate(request);

        String customAlias = request.getCustomAlias();
        if (customAlias != null) {
            return saveCustomAlias(request, customAlias);
        }

        for (int attempt = 0; attempt < MAX_GENERATED_CODE_ATTEMPTS; attempt++) {
            String shortCode = shortCodeGenerator.generate();
            if (urlRepository.existsByShortCode(shortCode)) {
                continue;
            }

            try {
                return save(request, shortCode);
            } catch (DataIntegrityViolationException exception) {
                // A concurrent insert may win between existsByShortCode and save.
            }
        }

        throw new IllegalStateException("Unable to generate a unique short code");
    }

    public String redirect(String shortCode) {
        CachedUrl cachedUrl = safeGetUrl(shortCode).orElse(null);
        if (cachedUrl != null) {
            if (isExpired(cachedUrl.expiresAt())) {
                safeInvalidate(shortCode);
                throw new ExpiredUrlException(shortCode);
            }
            safeIncrementClicks(shortCode);
            return cachedUrl.longUrl();
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        validateExpiry(shortCode, url.getExpiresAt());
        cache(url);
        safeIncrementClicks(shortCode);
        return url.getLongUrl();
    }

    public StatsResponse stats(String shortCode) {
        Url url = findByShortCode(shortCode);
        Long clicks = safeGetClicks(shortCode);
        if (clicks == null) {
            clicks = url.getClicks();
            safeInitializeClicks(shortCode, clicks);
        }
        return new StatsResponse(url.getLongUrl(), clicks, url.getCreatedAt(), url.getExpiresAt());
    }

    public void delete(String shortCode) {
        Url url = findByShortCode(shortCode);
        urlRepository.delete(url);
        safeInvalidate(shortCode);
    }

    private Url findByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    private void validateExpiry(String shortCode, Instant expiresAt) {
        if (isExpired(expiresAt)) {
            throw new ExpiredUrlException(shortCode);
        }
    }

    private boolean isExpired(Instant expiresAt) {
        return expiresAt != null && !expiresAt.isAfter(Instant.now());
    }

    private void cache(Url url) {
        Duration ttl = cacheTtl;
        if (url.getExpiresAt() != null) {
            ttl = Duration.between(Instant.now(), url.getExpiresAt());
        }
        if (!ttl.isZero() && !ttl.isNegative()) {
            try {
                redisService.cacheUrl(url.getShortCode(), new CachedUrl(url.getLongUrl(), url.getExpiresAt()), ttl);
            } catch (RuntimeException ignored) {
                // Redis is an optimization; PostgreSQL remains authoritative.
            }
        }
    }

    private Optional<CachedUrl> safeGetUrl(String shortCode) {
        try {
            return redisService.getUrl(shortCode);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Long safeGetClicks(String shortCode) {
        try {
            return redisService.getClicks(shortCode);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void safeIncrementClicks(String shortCode) {
        try {
            redisService.incrementClicks(shortCode);
        } catch (RuntimeException ignored) {
            // A failed counter must not prevent the redirect.
        }
    }

    private void safeInitializeClicks(String shortCode, long clicks) {
        try {
            redisService.initializeClicks(shortCode, clicks);
        } catch (RuntimeException ignored) {
            // A later stats request can retry initialization.
        }
    }

    private void safeInvalidate(String shortCode) {
        try {
            redisService.invalidate(shortCode);
        } catch (RuntimeException ignored) {
            // The database operation remains authoritative.
        }
    }

    private ShortenResponse saveCustomAlias(ShortenRequest request, String alias) {
        if (urlRepository.existsByShortCode(alias)) {
            throw new AliasAlreadyExistsException(alias);
        }

        try {
            return save(request, alias);
        } catch (DataIntegrityViolationException exception) {
            throw new AliasAlreadyExistsException(alias);
        }
    }

    private ShortenResponse save(ShortenRequest request, String shortCode) {
        Url url = new Url(shortCode, request.getLongUrl(), Instant.now(), request.getExpiresAt());
        urlRepository.save(url);
        safeInitializeClicks(shortCode, 0);
        return new ShortenResponse(baseUrl + "/" + shortCode);
    }

    private void validate(ShortenRequest request) {
        if (request == null || request.getLongUrl() == null || request.getLongUrl().isBlank()) {
            throw new IllegalArgumentException("longUrl must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(request.getLongUrl());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("longUrl must be a valid HTTP or HTTPS URL", exception);
        }

        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("longUrl must be a valid HTTP or HTTPS URL");
        }

        String customAlias = request.getCustomAlias();
        if (customAlias != null && !customAlias.matches(ALIAS_PATTERN)) {
            throw new IllegalArgumentException("customAlias must contain 3 to 64 Base62, underscore, or hyphen characters");
        }

        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
    }

    private static final class NoOpRedisService implements RedisService {
        @Override
        public java.util.Optional<CachedUrl> getUrl(String shortCode) {
            return java.util.Optional.empty();
        }

        @Override
        public void cacheUrl(String shortCode, CachedUrl url, Duration ttl) {
        }

        @Override
        public Long incrementClicks(String shortCode) {
            return null;
        }

        @Override
        public Long getClicks(String shortCode) {
            return null;
        }

        @Override
        public void initializeClicks(String shortCode, long clicks) {
        }

        @Override
        public void invalidate(String shortCode) {
        }
    }
}