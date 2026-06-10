package com.xinki.portfolio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinki.portfolio.config.AIConfig;
import com.xinki.portfolio.entity.ChatHistory;
import com.xinki.portfolio.entity.KnowledgeBase;
import com.xinki.portfolio.mapper.ChatHistoryMapper;
import com.xinki.portfolio.mapper.KnowledgeBaseMapper;
import com.xinki.portfolio.service.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final AIConfig aiConfig;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public Map<String, Object> chat(String sessionId, String message) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        ChatHistory userMsg = new ChatHistory();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        chatHistoryMapper.insert(userMsg);

        String context = searchKnowledge(message);

        String systemPrompt = "你是 Xinki 的个人 AI 助手，一位全栈开发者的智能伙伴。" +
                "请用中文回答，风格简洁友好。" +
                (context != null ? "以下是你可能需要的背景信息：\n" + context : "");

        String reply = callOpenAI(systemPrompt, message, sessionId);

        ChatHistory assistantMsg = new ChatHistory();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(reply);
        chatHistoryMapper.insert(assistantMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("role", "assistant");
        result.put("content", reply);
        return result;
    }

    private String searchKnowledge(String query) {
        List<KnowledgeBase> all = knowledgeBaseMapper.selectList(null);
        if (all.isEmpty()) return null;

        return all.stream()
                .filter(kb -> {
                    String q = kb.getQuestion().toLowerCase();
                    String m = query.toLowerCase();
                    for (String word : m.split("\\s+")) {
                        if (q.contains(word)) return true;
                    }
                    return q.contains(m);
                })
                .map(kb -> "Q: " + kb.getQuestion() + "\nA: " + kb.getAnswer())
                .collect(Collectors.joining("\n\n"));
    }

    private String callOpenAI(String systemPrompt, String userMessage, String sessionId) {
        try {
            List<ChatHistory> history = chatHistoryMapper.selectList(
                    new LambdaQueryWrapper<ChatHistory>()
                            .eq(ChatHistory::getSessionId, sessionId)
                            .orderByAsc(ChatHistory::getCreatedAt)
                            .last("LIMIT 10"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            for (ChatHistory h : history) {
                messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = Map.of(
                    "model", aiConfig.getModel(),
                    "messages", messages,
                    "temperature", 0.7,
                    "max_tokens", 500
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Bailian API call failed", e);
            return "抱歉，我暂时无法回答，请稍后再试。";
        }
    }

    @Override
    public List<Map<String, Object>> getHistory(String sessionId) {
        return chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getSessionId, sessionId)
                        .orderByAsc(ChatHistory::getCreatedAt))
                .stream().map(h -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("role", h.getRole());
                    m.put("content", h.getContent());
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public void clearHistory(String sessionId) {
        chatHistoryMapper.delete(
                new LambdaQueryWrapper<ChatHistory>().eq(ChatHistory::getSessionId, sessionId));
    }
}