package com.careeros.resumetailor.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:beta-access-test;DB_CLOSE_DELAY=-1",
        "app.security.api-key-enabled=false",
        "app.security.beta-access-enabled=true",
        "app.security.beta-users=alice:alice-secret,bob:bob-secret",
        "app.security.rate-limit-per-minute=100",
        "app.security.tailor-rate-limit-per-hour=1",
        "app.security.tailor-rate-limit-per-month=2"
})
class BetaAccessFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void accountRequiresValidIndividualCode() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/account").header("X-Beta-Token", "alice-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("alice"))
                .andExpect(jsonPath("$.monthlyRemaining").value(2));
    }

    @Test
    void quotaIsCountedByUser() throws Exception {
        mockMvc.perform(tailorRequest("bob-secret"))
                .andExpect(status().isBadRequest()); // quota passed; required JD part intentionally omitted

        mockMvc.perform(tailorRequest("bob-secret"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Your beta resume-generation quota has been reached."));
    }

    private static org.springframework.test.web.servlet.RequestBuilder tailorRequest(String token) {
        return multipart("/api/tailor")
                .file(new MockMultipartFile("resume", "r.txt", "text/plain", "hello".getBytes()))
                .header("X-Beta-Token", token);
    }
}
