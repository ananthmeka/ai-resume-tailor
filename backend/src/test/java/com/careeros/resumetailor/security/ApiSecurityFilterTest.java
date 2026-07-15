package com.careeros.resumetailor.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.api-key-enabled=true",
        "app.security.api-key=test-secret-key",
        "app.security.rate-limit-per-minute=100",
        "app.security.tailor-rate-limit-per-hour=100"
})
class ApiSecurityFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void tailorRejectsMissingKey() throws Exception {
        mockMvc.perform(multipart("/api/tailor")
                        .file(new MockMultipartFile("resume", "r.txt", "text/plain", "hello".getBytes()))
                        .part(new MockPart("jobDescription", "java job".getBytes())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void tailorAcceptsHeaderKey() throws Exception {
        mockMvc.perform(multipart("/api/tailor")
                        .file(new MockMultipartFile("resume", "r.txt", "text/plain", "hello".getBytes()))
                        .part(new MockPart("jobDescription", "java job".getBytes()))
                        .header("X-API-Key", "test-secret-key"))
                .andExpect(status().is5xxServerError()); // LLM not configured in test — passed auth
    }
}
