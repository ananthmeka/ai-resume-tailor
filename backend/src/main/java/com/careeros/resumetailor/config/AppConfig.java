package com.careeros.resumetailor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    RestClient openAiRestClient(
            @Value("${app.openai.base-url}") String baseUrl,
            @Value("${app.openai.timeout-seconds}") int timeoutSeconds) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
                    setReadTimeout(Duration.ofSeconds(timeoutSeconds));
                }})
                .build();
    }

    @Bean
    CorsFilter corsFilter(@Value("${app.cors-origins}") String corsOrigins) {
        List<String> origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        boolean usePatterns = origins.stream().anyMatch(o -> o.contains("*"));
        if (usePatterns) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOrigins(origins);
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
