package com.xinki.portfolio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "bailian")
public class AIConfig {
    private String apiKey;
    private String model;
}
