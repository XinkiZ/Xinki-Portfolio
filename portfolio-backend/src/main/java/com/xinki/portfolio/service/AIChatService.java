package com.xinki.portfolio.service;

import java.util.List;
import java.util.Map;

public interface AIChatService {
    Map<String, Object> chat(String sessionId, String message);
    List<Map<String, Object>> getHistory(String sessionId);
    void clearHistory(String sessionId);
}
