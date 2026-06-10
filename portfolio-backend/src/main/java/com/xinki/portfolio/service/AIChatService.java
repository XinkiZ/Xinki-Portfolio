package com.xinki.portfolio.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AIChatService {
    Map<String, Object> chat(String sessionId, String message);
    SseEmitter chatStream(String sessionId, String message);
    List<Map<String, Object>> getHistory(String sessionId);
    void clearHistory(String sessionId);
}