package com.xinki.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinki.portfolio.config.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String EMBEDDING_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

    /**
     * Generate embedding vector for a single text.
     * Returns 1024-dim float array, or null on failure.
     */
    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> body = Map.of(
                    "model", aiConfig.getEmbeddingModel(),
                    "input", Collections.singletonList(text)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EMBEDDING_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();
            JsonNode root = objectMapper.readTree(responseBody);

            // Check for API-level errors first (DashScope nests errors under "error" key)
            if (root.has("error")) {
                JsonNode err = root.path("error");
                String errCode = err.path("code").asText("");
                String errMsg = err.path("message").asText("");
                log.error("Embedding API error (HTTP {}): code={}, message={}", statusCode, errCode, errMsg);
                return null;
            }
            if (root.has("code") && !"200".equals(root.path("code").asText())) {
                String apiMsg = root.path("message").asText("");
                log.error("Embedding API error (HTTP {}): code={}, message={}", statusCode,
                        root.path("code").asText(), apiMsg);
                return null;
            }

            JsonNode data = root.path("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode vec = data.get(0).path("embedding");
                float[] result = new float[vec.size()];
                for (int i = 0; i < vec.size(); i++) {
                    result[i] = (float) vec.get(i).asDouble();
                }
                return result;
            }
            log.warn("Empty embedding result (HTTP {}) for text \"{}\": {}",
                    statusCode, text.substring(0, Math.min(50, text.length())),
                    responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);
            return null;
        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage());
            return null;
        }
    }

    /** Serialize float[] to JSON string for DB storage. */
    public String serialize(float[] vec) {
        if (vec == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /** Deserialize JSON string back to float[]. */
    public float[] deserialize(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JsonNode arr = objectMapper.readTree(json);
            float[] result = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                result[i] = (float) arr.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to deserialize embedding: {}", e.getMessage());
            return null;
        }
    }
}