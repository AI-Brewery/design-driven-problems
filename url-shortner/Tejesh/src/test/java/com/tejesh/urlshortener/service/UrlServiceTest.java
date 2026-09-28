package com.tejesh.urlshortener.service;

import com.tejesh.urlshortener.cache.CachedUrl;
import com.tejesh.urlshortener.cache.RedisService;
import com.tejesh.urlshortener.dto.ShortenRequest;
import com.tejesh.urlshortener.entity.Url;
import com.tejesh.urlshortener.exception.AliasAlreadyExistsException;
import com.tejesh.urlshortener.exception.ExpiredUrlException;
import com.tejesh.urlshortener.exception.UrlNotFoundException;
import com.tejesh.urlshortener.repository.UrlRepository;
import com.tejesh.urlshortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private RedisService redisService;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, shortCodeGenerator, redisService,
            "http://localhost:8080", Duration.ofHours(1));
    }

    @Test
    void createsUrlWithGeneratedCode() {
        when(shortCodeGenerator.generate()).thenReturn("aB3xK9Q");
        when(urlRepository.existsByShortCode("aB3xK9Q")).thenReturn(false);

        var response = urlService.shorten(request("https://example.com/article"));

        assertEquals("http://localhost:8080/aB3xK9Q", response.getShortUrl());
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void createsUrlWithCustomAlias() {
        when(urlRepository.existsByShortCode("my-blog")).thenReturn(false);

        var response = urlService.shorten(requestWithAlias("my-blog"));

        assertEquals("http://localhost:8080/my-blog", response.getShortUrl());
        verify(urlRepository).save(any(Url.class));
        verify(shortCodeGenerator, never()).generate();
    }

    @Test
    void rejectsDuplicateAlias() {
        when(urlRepository.existsByShortCode("my-blog")).thenReturn(true);

        assertThrows(AliasAlreadyExistsException.class,
                () -> urlService.shorten(requestWithAlias("my-blog")));
    }

    @Test
    void retriesGeneratedCodeAfterDatabaseCollision() {
        when(shortCodeGenerator.generate()).thenReturn("first01", "second2");
        when(urlRepository.existsByShortCode("first01")).thenReturn(false);
        when(urlRepository.existsByShortCode("second2")).thenReturn(false);
        when(urlRepository.save(any(Url.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = urlService.shorten(request("https://example.com/article"));

        assertEquals("http://localhost:8080/second2", response.getShortUrl());
        verify(shortCodeGenerator, org.mockito.Mockito.times(2)).generate();
    }

    @Test
    void rejectsInvalidUrlAliasAndExpiry() {
        ShortenRequest invalidUrl = request("ftp://example.com/file");
        ShortenRequest invalidAlias = requestWithAlias("bad alias");
        ShortenRequest invalidExpiry = request("https://example.com");
        invalidExpiry.setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(IllegalArgumentException.class, () -> urlService.shorten(invalidUrl));
        assertThrows(IllegalArgumentException.class, () -> urlService.shorten(invalidAlias));
        assertThrows(IllegalArgumentException.class, () -> urlService.shorten(invalidExpiry));
    }

    @Test
    void redirectsOnCacheMissAndIncrementsRedisClicks() {
        Url url = new Url("aB3xK9Q", "https://example.com", Instant.now(), null);
        when(urlRepository.findByShortCode("aB3xK9Q")).thenReturn(Optional.of(url));

        assertEquals("https://example.com", urlService.redirect("aB3xK9Q"));
        verify(redisService).cacheUrl("aB3xK9Q", new CachedUrl("https://example.com", null), Duration.ofHours(1));
        verify(redisService).incrementClicks("aB3xK9Q");
        verify(urlRepository, never()).save(url);
    }

    @Test
    void redirectsOnCacheHitWithoutPostgresLookup() {
        when(redisService.getUrl("aB3xK9Q"))
                .thenReturn(Optional.of(new CachedUrl("https://example.com/cached", null)));

        assertEquals("https://example.com/cached", urlService.redirect("aB3xK9Q"));
        verify(urlRepository, never()).findByShortCode("aB3xK9Q");
        verify(redisService).incrementClicks("aB3xK9Q");
    }

    @Test
    void usesExpiryAsCacheTtl() {
        Instant expiresAt = Instant.now().plusSeconds(120);
        Url url = new Url("expiring", "https://example.com", Instant.now(), expiresAt);
        when(urlRepository.findByShortCode("expiring")).thenReturn(Optional.of(url));

        urlService.redirect("expiring");

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(redisService).cacheUrl(eq("expiring"),
            eq(new CachedUrl("https://example.com", expiresAt)), ttl.capture());
        org.junit.jupiter.api.Assertions.assertTrue(ttl.getValue().compareTo(Duration.ZERO) > 0);
        org.junit.jupiter.api.Assertions.assertTrue(ttl.getValue().compareTo(Duration.ofSeconds(120)) <= 0);
    }

    @Test
    void rejectsMissingAndExpiredUrls() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());
        assertThrows(UrlNotFoundException.class, () -> urlService.redirect("missing"));

        Url expired = new Url("expired", "https://example.com", Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(1));
        when(urlRepository.findByShortCode("expired")).thenReturn(Optional.of(expired));
        assertThrows(ExpiredUrlException.class, () -> urlService.redirect("expired"));
    }

    @Test
    void rejectsExpiredCachedUrlAndInvalidatesIt() {
        when(redisService.getUrl("expired"))
                .thenReturn(Optional.of(new CachedUrl("https://example.com", Instant.now().minusSeconds(1))));

        assertThrows(ExpiredUrlException.class, () -> urlService.redirect("expired"));
        verify(redisService).invalidate("expired");
        verify(urlRepository, never()).findByShortCode("expired");
    }

    @Test
    void incrementsRedisForMultipleClicks() {
        when(redisService.getUrl("popular"))
                .thenReturn(Optional.of(new CachedUrl("https://example.com", null)));

        urlService.redirect("popular");
        urlService.redirect("popular");
        urlService.redirect("popular");

        verify(redisService, org.mockito.Mockito.times(3)).incrementClicks("popular");
    }

    @Test
    void returnsStatsAndDeletesUrl() {
        Url url = new Url("aB3xK9Q", "https://example.com", Instant.now(), null);
        url.setClicks(10);
        when(urlRepository.findByShortCode("aB3xK9Q")).thenReturn(Optional.of(url));
        when(redisService.getClicks("aB3xK9Q")).thenReturn(null);

        var stats = urlService.stats("aB3xK9Q");
        assertEquals("https://example.com", stats.getLongUrl());
        assertEquals(10, stats.getClicks());

        urlService.delete("aB3xK9Q");
        verify(urlRepository).delete(url);
        verify(redisService).invalidate("aB3xK9Q");
    }

    @Test
    void statsUsesRedisCounterWhenAvailable() {
        Url url = new Url("stats", "https://example.com", Instant.now(), null);
        when(urlRepository.findByShortCode("stats")).thenReturn(Optional.of(url));
        when(redisService.getClicks("stats")).thenReturn(7L);

        assertEquals(7, urlService.stats("stats").getClicks());
        verify(redisService, never()).initializeClicks(anyString(), anyLong());
    }

    @Test
    void statsFallsBackToPostgresAndInitializesRedisCounter() {
        Url url = new Url("stats", "https://example.com", Instant.now(), null);
        url.setClicks(4);
        when(urlRepository.findByShortCode("stats")).thenReturn(Optional.of(url));
        when(redisService.getClicks("stats")).thenReturn(null);

        assertEquals(4, urlService.stats("stats").getClicks());
        verify(redisService).initializeClicks("stats", 4);
    }

    @Test
    void redisFailureFallsBackToPostgresForRedirect() {
        Url url = new Url("fallback", "https://example.com", Instant.now(), null);
        when(redisService.getUrl("fallback")).thenThrow(new RuntimeException("redis unavailable"));
        when(urlRepository.findByShortCode("fallback")).thenReturn(Optional.of(url));

        assertEquals("https://example.com", urlService.redirect("fallback"));
        verify(urlRepository).findByShortCode("fallback");
    }

    private ShortenRequest request(String longUrl) {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl(longUrl);
        return request;
    }

    private ShortenRequest requestWithAlias(String alias) {
        ShortenRequest request = request("https://example.com/article");
        request.setCustomAlias(alias);
        return request;
    }
}