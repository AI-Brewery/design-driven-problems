package com.tejesh.urlshortener.service;

import com.tejesh.urlshortener.cache.RedisService;
import com.tejesh.urlshortener.entity.Url;
import com.tejesh.urlshortener.repository.UrlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UrlBackgroundJobs {

    private final UrlRepository urlRepository;
    private final RedisService redisService;

    public UrlBackgroundJobs(UrlRepository urlRepository, RedisService redisService) {
        this.urlRepository = urlRepository;
        this.redisService = redisService;
    }

    @Scheduled(
            fixedDelayString = "${url-shortener.jobs.click-persistence-delay:10000}",
            initialDelayString = "${url-shortener.jobs.click-persistence-initial-delay:10000}")
    public void persistClickCounts() {
        try {
            for (Url url : urlRepository.findAll()) {
                persistClickCount(url);
            }
        } catch (RuntimeException ignored) {
            // A database or Redis outage is retried by the next scheduled run.
        }
    }

    @Scheduled(
            fixedDelayString = "${url-shortener.jobs.expiry-cleanup-delay:60000}",
            initialDelayString = "${url-shortener.jobs.expiry-cleanup-initial-delay:60000}")
    public void deleteExpiredUrls() {
        try {
            for (Url url : urlRepository.findByExpiresAtBefore(Instant.now())) {
                try {
                    urlRepository.delete(url);
                } catch (RuntimeException ignored) {
                    // Continue cleaning other records; this record is retried later.
                }
            }
        } catch (RuntimeException ignored) {
            // A database outage is retried by the next scheduled run.
        }
    }

    private void persistClickCount(Url url) {
        try {
            Long clicks = redisService.getClicks(url.getShortCode());
            if (clicks != null && clicks >= 0) {
                // Absolute replacement is idempotent and a deleted row updates zero rows.
                urlRepository.updateClicksByShortCode(url.getShortCode(), clicks);
            }
        } catch (RuntimeException ignored) {
            // A bad key or failed record must not stop the remaining records.
        }
    }
}