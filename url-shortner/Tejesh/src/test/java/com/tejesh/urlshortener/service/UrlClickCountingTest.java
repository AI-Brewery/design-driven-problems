package com.tejesh.urlshortener.service;

import com.tejesh.urlshortener.cache.CachedUrl;
import com.tejesh.urlshortener.cache.RedisService;
import com.tejesh.urlshortener.dto.ShortenRequest;
import com.tejesh.urlshortener.entity.Url;
import com.tejesh.urlshortener.exception.ExpiredUrlException;
import com.tejesh.urlshortener.exception.UrlNotFoundException;
import com.tejesh.urlshortener.repository.UrlRepository;
import com.tejesh.urlshortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlClickCountingTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    private InMemoryRedisService redisService;
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        redisService = new InMemoryRedisService();
        urlService = new UrlService(urlRepository, shortCodeGenerator, redisService,
                "http://localhost:8080", Duration.ofHours(1));
    }

    @Test
    void firstRedirectIsVisibleInStats() {
        Url url = url("first");
        when(urlRepository.findByShortCode("first")).thenReturn(Optional.of(url));

        urlService.redirect("first");

        assertEquals(1, urlService.stats("first").getClicks());
    }

    @Test
    void threeRedirectsProduceThreeClicks() {
        Url url = url("popular");
        when(urlRepository.findByShortCode("popular")).thenReturn(Optional.of(url));

        urlService.redirect("popular");
        urlService.redirect("popular");
        urlService.redirect("popular");

        assertEquals(3, urlService.stats("popular").getClicks());
    }

    @Test
    void cacheHitIncrementsWithoutPostgresRedirectLookup() {
        redisService.cacheUrl("cached", new CachedUrl("https://example.com/cached", null), Duration.ofHours(1));

        urlService.redirect("cached");

        assertEquals(1, redisService.getClicks("cached"));
        verify(urlRepository, never()).findByShortCode("cached");
    }

    @Test
    void cacheMissPopulatesCacheAndIncrementsCounter() {
        Url url = url("miss");
        when(urlRepository.findByShortCode("miss")).thenReturn(Optional.of(url));

        urlService.redirect("miss");

        assertEquals(new CachedUrl(url.getLongUrl(), null), redisService.urls.get("url:miss"));
        assertEquals(1, redisService.getClicks("miss"));
    }

    @Test
    void expiredUrlDoesNotIncrementCounter() {
        Url url = new Url("expired", "https://example.com", Instant.now(), Instant.now().minusSeconds(1));
        when(urlRepository.findByShortCode("expired")).thenReturn(Optional.of(url));

        assertThrows(ExpiredUrlException.class, () -> urlService.redirect("expired"));

        assertEquals(null, redisService.getClicks("expired"));
    }

    @Test
    void deletedUrlCannotContinueIncrementingCounter() {
        Url url = url("deleted");
        AtomicBoolean present = new AtomicBoolean(true);
        when(urlRepository.findByShortCode("deleted"))
            .thenAnswer(invocation -> present.getAndSet(false) ? Optional.of(url) : Optional.empty());
        redisService.cacheUrl("deleted", new CachedUrl(url.getLongUrl(), null), Duration.ofHours(1));

        urlService.delete("deleted");

        assertThrows(UrlNotFoundException.class, () -> urlService.redirect("deleted"));
        assertEquals(null, redisService.getClicks("deleted"));
    }

    @Test
    void newUrlInitializesRedisCounterToZero() {
        when(shortCodeGenerator.generate()).thenReturn("new-url");
        when(urlRepository.existsByShortCode("new-url")).thenReturn(false);

        urlService.shorten(request("https://example.com/new"));

        assertEquals(0, redisService.getClicks("new-url"));
    }

    private Url url(String shortCode) {
        return new Url(shortCode, "https://example.com/" + shortCode, Instant.now(), null);
    }

    private ShortenRequest request(String longUrl) {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl(longUrl);
        return request;
    }

    private static final class InMemoryRedisService implements RedisService {
        private final Map<String, CachedUrl> urls = new HashMap<>();
        private final Map<String, Long> clicks = new HashMap<>();

        @Override
        public Optional<CachedUrl> getUrl(String shortCode) {
            return Optional.ofNullable(urls.get("url:" + shortCode));
        }

        @Override
        public void cacheUrl(String shortCode, CachedUrl url, Duration ttl) {
            urls.put("url:" + shortCode, url);
        }

        @Override
        public Long incrementClicks(String shortCode) {
            long next = clicks.getOrDefault("clicks:" + shortCode, 0L) + 1;
            clicks.put("clicks:" + shortCode, next);
            return next;
        }

        @Override
        public Long getClicks(String shortCode) {
            return clicks.get("clicks:" + shortCode);
        }

        @Override
        public void initializeClicks(String shortCode, long count) {
            clicks.putIfAbsent("clicks:" + shortCode, count);
        }

        @Override
        public void invalidate(String shortCode) {
            urls.remove("url:" + shortCode);
            clicks.remove("clicks:" + shortCode);
        }
    }
}
