package com.tejesh.urlshortener.controller;

import com.tejesh.urlshortener.dto.ShortenRequest;
import com.tejesh.urlshortener.dto.ShortenResponse;
import com.tejesh.urlshortener.dto.StatsResponse;
import com.tejesh.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.shorten(request));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(urlService.redirect(shortCode)))
                .build();
    }

    @GetMapping("/stats/{shortCode}")
    public StatsResponse stats(@PathVariable String shortCode) {
        return urlService.stats(shortCode);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> delete(@PathVariable String shortCode) {
        urlService.delete(shortCode);
        return ResponseEntity.noContent().build();
    }
}