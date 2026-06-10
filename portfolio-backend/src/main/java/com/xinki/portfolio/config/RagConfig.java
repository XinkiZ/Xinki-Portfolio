package com.xinki.portfolio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private int topK = 3;
    private int maxChunksPerFile = 50;
    private int minChunkLength = 50;
    private int maxChunkLength = 800;
    private int maxContextChars = 2000;
}