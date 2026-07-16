package com.careeros.resumetailor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.careeros.resumetailor.config.LlmLimits;

@SpringBootApplication
@EnableConfigurationProperties(LlmLimits.class)
public class ResumeTailorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeTailorApplication.class, args);
    }
}
