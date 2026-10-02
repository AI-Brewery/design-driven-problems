package com.tejesh.urlshortener;

import com.tejesh.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlApiIntegrationTest {

    private static final String ALIAS = "integration-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlRepository urlRepository;

    @BeforeEach
    @AfterEach
    void removeFixture() {
        urlRepository.findByShortCode(ALIAS).ifPresent(urlRepository::delete);
    }

    @Test
    void supportsCreateRedirectStatsAndDeleteAgainstPostgres() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType("application/json")
                        .content("{\"longUrl\":\"https://example.com/integration\",\"customAlias\":\""
                                + ALIAS + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/" + ALIAS));

        mockMvc.perform(get("/stats/" + ALIAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(0))
                .andExpect(jsonPath("$.longUrl").value("https://example.com/integration"));

        mockMvc.perform(get("/" + ALIAS))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/integration"));

        mockMvc.perform(delete("/" + ALIAS))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/stats/" + ALIAS))
                .andExpect(status().isNotFound());
    }
}