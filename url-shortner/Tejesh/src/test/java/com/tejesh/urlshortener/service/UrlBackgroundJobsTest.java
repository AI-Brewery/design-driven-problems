package com.tejesh.urlshortener.service;

import com.tejesh.urlshortener.cache.RedisService;
import com.tejesh.urlshortener.entity.Url;
import com.tejesh.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlBackgroundJobsTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedisService redisService;

    private UrlBackgroundJobs jobs;

    @BeforeEach
    void setUp() {
        jobs = new UrlBackgroundJobs(urlRepository, redisService);
    }

    @Test
    void persistsAbsoluteRedisCountsAndSkipsMissingKeys() {
        Url counted = url("counted");
        Url missing = url("missing");
        when(urlRepository.findAll()).thenReturn(List.of(counted, missing));
        when(redisService.getClicks("counted")).thenReturn(12L);
        when(redisService.getClicks("missing")).thenReturn(null);

        jobs.persistClickCounts();
        jobs.persistClickCounts();

        verify(urlRepository, org.mockito.Mockito.times(2))
                .updateClicksByShortCode("counted", 12L);
        verify(urlRepository, never()).updateClicksByShortCode("missing", 0L);
    }

    @Test
    void continuesAfterRedisOrDatabaseFailureForOneRecord() {
        Url failed = url("failed");
        Url successful = url("successful");
        when(urlRepository.findAll()).thenReturn(List.of(failed, successful));
        when(redisService.getClicks("failed")).thenThrow(new RuntimeException("redis down"));
        when(redisService.getClicks("successful")).thenReturn(4L);

        jobs.persistClickCounts();

        verify(urlRepository).updateClicksByShortCode("successful", 4L);
    }

    @Test
    void cleansOnlyExpiredRecordsAndSurvivesDeleteFailure() {
        Url expired = url("expired");
        Url active = url("active");
        when(urlRepository.findByExpiresAtBefore(any(Instant.class))).thenReturn(List.of(expired, active));

        jobs.deleteExpiredUrls();

        verify(urlRepository).delete(expired);
        verify(urlRepository).delete(active);
    }

    private Url url(String shortCode) {
        return new Url(shortCode, "https://example.com", Instant.now(), null);
    }
}