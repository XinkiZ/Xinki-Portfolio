package com.xinki.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * DashScope native embedding API service.
 * Uses DashScope's own API (not OpenAI-compatible mode) for text embeddings.
 * Endpoint: https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding
 */
@Slf4j
@Service
public class DashScopeEmbeddingService {

    private static final String EMBEDDING_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DashScopeEmbeddingService(
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate embedding for a query text.
     */
    public float[] embed(String text) {
        return embed(text, "query");
    }

    /**
     * Generate embedding for document content (used for indexing).
     */
    public float[] embedDocument(String text) {
        return embed(text, "document");
    }

    private float[] embed(String text, String textType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "text-embedding-v3");
            ObjectNode input = body.putObject("input");
            ArrayNode texts = input.putArray("texts");
            texts.add(text);
            ObjectNode params = body.putObject("parameters");
            params.put("text_type", textType);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddings = root.path("output").path("embeddings");
                if (embeddings.isArray() && embeddings.size() > 0) {
                    JsonNode embedding = embeddings.get(0).path("embedding");
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = (float) embedding.get(i).asDouble();
                    }
                    return result;
                }
            }
            log.error("DashScope embedding API unexpected response: {}", response.getBody());
        } catch (Exception e) {
            log.error("DashScope embedding API call failed", e);
        }
        return null;
    }
}