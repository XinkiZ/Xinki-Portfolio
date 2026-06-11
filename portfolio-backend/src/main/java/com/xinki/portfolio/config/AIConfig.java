package com.xinki.portfolio.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "bailian")
public class AIConfig {

    @PostConstruct
    public void init() {
        String key = apiKey;
        if (key == null || key.isEmpty()) {
            log.error("BAILIAN_API_KEY is EMPTY — AI features will fail");
        } else {
            log.info("BAILIAN_API_KEY loaded (length={}, starts with: {}***)",
                    key.length(), key.substring(0, Math.min(8, key.length())));
        }
    }

    private String apiKey;
    private String model;
    private String embeddingModel;
}