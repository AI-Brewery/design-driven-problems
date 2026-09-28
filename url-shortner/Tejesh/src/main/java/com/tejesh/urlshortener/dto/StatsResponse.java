package com.tejesh.urlshortener.dto;

import java.time.Instant;

public class StatsResponse {

    private String longUrl;
    private long clicks;
    private Instant createdAt;
    private Instant expiresAt;

    public StatsResponse() {
    }

    public StatsResponse(String longUrl, long clicks, Instant createdAt, Instant expiresAt) {
        this.longUrl = longUrl;
        this.clicks = clicks;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}