package com.tejesh.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejesh.urlshortener.dto.ShortenResponse;
import com.tejesh.urlshortener.dto.StatsResponse;
import com.tejesh.urlshortener.exception.GlobalExceptionHandler;
import com.tejesh.urlshortener.exception.UrlNotFoundException;
import com.tejesh.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
@Import(GlobalExceptionHandler.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private UrlService urlService;

    @Test
    void exposesCreateRedirectStatsAndDeleteApis() throws Exception {
        when(urlService.shorten(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ShortenResponse("http://localhost:8080/aB3xK9Q"));
        when(urlService.redirect("aB3xK9Q")).thenReturn("https://example.com/article");
        when(urlService.stats("aB3xK9Q")).thenReturn(
                new StatsResponse("https://example.com/article", 10, Instant.now(), null));
        doNothing().when(urlService).delete("aB3xK9Q");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(java.util.Map.of(
                                "longUrl", "https://example.com/article"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/aB3xK9Q"));

        mockMvc.perform(get("/aB3xK9Q"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/article"));

        mockMvc.perform(get("/stats/aB3xK9Q"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(10));

        mockMvc.perform(delete("/aB3xK9Q"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsConsistentErrors() throws Exception {
        when(urlService.redirect("missing")).thenThrow(new UrlNotFoundException("missing"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("URL_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }
}