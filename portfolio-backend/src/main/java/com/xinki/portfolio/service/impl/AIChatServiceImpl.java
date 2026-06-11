package com.xinki.portfolio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinki.portfolio.config.RagConfig;
import com.xinki.portfolio.entity.ChatHistory;
import com.xinki.portfolio.entity.KnowledgeBase;
import com.xinki.portfolio.entity.Project;
import com.xinki.portfolio.entity.Skill;
import com.xinki.portfolio.mapper.ChatHistoryMapper;
import com.xinki.portfolio.mapper.KnowledgeBaseMapper;
import com.xinki.portfolio.mapper.ProjectMapper;
import com.xinki.portfolio.mapper.SkillMapper;
import com.xinki.portfolio.service.AIChatService;
import com.xinki.portfolio.service.VectorCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final EmbeddingModel embeddingModel;
    private final RagConfig ragConfig;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ProjectMapper projectMapper;
    private final SkillMapper skillMapper;
    private final VectorCacheService vectorCacheService;

    private static final int HISTORY_LIMIT = 10;

    // ==================== 非流式 ====================

    @Override
    public Map<String, Object> chat(String sessionId, String message) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        // 1. Query history BEFORE saving current message (avoids dedup)
        List<ChatHistory> history = getRecentHistory(sessionId);

        // 2. Save user message
        ChatHistory userMsg = new ChatHistory();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        chatHistoryMapper.insert(userMsg);

        // 3. RAG context
        String ragContext = searchKnowledge(message);
        String catalog = buildCatalog();
        String context = mergeContext(ragContext, catalog);

        // 4. Build history messages for Spring AI
        List<Message> historyMessages = new ArrayList<>();
        for (ChatHistory h : history) {
            if ("user".equals(h.getRole())) {
                historyMessages.add(new UserMessage(h.getContent()));
            } else {
                historyMessages.add(new AssistantMessage(h.getContent()));
            }
        }

        // 5. Call Spring AI ChatClient
        String reply;
        try {
            reply = chatClientBuilder.build().prompt()
                    .system(buildSystemPrompt(context))
                    .messages(historyMessages)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Chat API call failed", e);
            reply = "抱歉，我暂时无法回答，请稍后再试。";
        }

        // 6. Save assistant reply
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

    // ==================== 流式 SSE ====================

    @Override
    public SseEmitter chatStream(String sessionId, String message) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        final String finalSessionId = sessionId;

        // 1. Query history BEFORE saving current message
        List<ChatHistory> history = getRecentHistory(finalSessionId);

        // 2. Save user message
        ChatHistory userMsg = new ChatHistory();
        userMsg.setSessionId(finalSessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        chatHistoryMapper.insert(userMsg);

        // 3. RAG context
        String ragContext = searchKnowledge(message);
        String catalog = buildCatalog();
        String context = mergeContext(ragContext, catalog);
        String systemPrompt = buildSystemPrompt(context);

        // 4. Build history messages
        List<Message> historyMessages = new ArrayList<>();
        for (ChatHistory h : history) {
            if ("user".equals(h.getRole())) {
                historyMessages.add(new UserMessage(h.getContent()));
            } else {
                historyMessages.add(new AssistantMessage(h.getContent()));
            }
        }

        SseEmitter emitter = new SseEmitter(120_000L);

        // 5. Stream via Spring AI ChatClient
        StringBuilder fullReply = new StringBuilder();
        chatClientBuilder.build().prompt()
                .system(systemPrompt)
                .messages(historyMessages)
                .user(message)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    fullReply.append(chunk);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(Map.of("type", "chunk", "content", chunk)));
                    } catch (Exception ignored) {}
                })
                .doOnComplete(() -> {
                    try {
                        if (fullReply.length() > 0) {
                            ChatHistory assistantMsg = new ChatHistory();
                            assistantMsg.setSessionId(finalSessionId);
                            assistantMsg.setRole("assistant");
                            assistantMsg.setContent(fullReply.toString());
                            chatHistoryMapper.insert(assistantMsg);
                        }
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"sessionId\":\"" + finalSessionId + "\"}"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Failed to complete SSE stream", e);
                        try { emitter.complete(); } catch (Exception ignored) {}
                    }
                })
                .doOnError(e -> {
                    log.error("Stream chat failed", e);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("type", "error", "message", "抱歉，我暂时无法回答，请稍后再试。")));
                        emitter.complete();
                    } catch (Exception ignored) {}
                })
                .subscribe();

        return emitter;
    }

    // ==================== RAG 检索 ====================

    private String searchKnowledge(String query) {
        List<KnowledgeBase> all = knowledgeBaseMapper.selectList(null);
        if (all.isEmpty()) return null;

        // Generate query embedding via Spring AI EmbeddingModel
        float[] queryVec;
        try {
            queryVec = embeddingModel.embed(query);
        } catch (Exception e) {
            log.warn("Embedding failed, falling back to keyword search", e);
            return fallbackKeywordSearch(query, all);
        }

        List<Long> ids = all.stream().map(KnowledgeBase::getId).toList();
        Map<Long, float[]> cache = vectorCacheService.getAll(ids);

        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeBase kb : all) {
            float[] kbVec = cache.get(kb.getId());
            if (kbVec == null) continue;
            double score = cosineSimilarity(queryVec, kbVec);
            scored.add(new ScoredChunk(kb, score));
        }

        if (scored.isEmpty()) return null;

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int topK = Math.min(ragConfig.getTopK(), scored.size());

        StringBuilder context = new StringBuilder();
        int totalChars = 0;
        for (int i = 0; i < topK; i++) {
            ScoredChunk sc = scored.get(i);
            String chunk = sc.kb.getContent();
            if (chunk == null) continue;
            if (totalChars + chunk.length() > ragConfig.getMaxContextChars()) {
                int remaining = ragConfig.getMaxContextChars() - totalChars;
                if (remaining > 50) context.append(chunk, 0, remaining).append("...");
                break;
            }
            if (context.length() > 0) context.append("\n\n---\n\n");
            context.append(chunk);
            totalChars += chunk.length();
        }
        return context.length() > 0 ? context.toString() : null;
    }

    private String fallbackKeywordSearch(String query, List<KnowledgeBase> all) {
        return all.stream()
                .filter(kb -> {
                    String c = kb.getContent().toLowerCase();
                    String m = query.toLowerCase();
                    for (String word : m.split("\\s+")) {
                        if (c.contains(word)) return true;
                    }
                    return c.contains(m);
                })
                .limit(ragConfig.getTopK())
                .map(KnowledgeBase::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ==================== 助手方法 ====================

    /** Query the most recent N messages in chronological order (BEFORE current message is saved). */
    private List<ChatHistory> getRecentHistory(String sessionId) {
        List<ChatHistory> history = chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getSessionId, sessionId)
                        .orderByDesc(ChatHistory::getCreatedAt)
                        .last("LIMIT " + HISTORY_LIMIT));
        java.util.Collections.reverse(history);
        log.debug("Loaded {} history messages for session {}", history.size(), sessionId);
        return history;
    }

    /** Build compact catalog of all published projects and skills. */
    private String buildCatalog() {
        StringBuilder sb = new StringBuilder();

        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getIsPublished, 1).orderByDesc(Project::getSortOrder));
        if (!projects.isEmpty()) {
            sb.append("## 作品列表\n");
            for (int i = 0; i < projects.size(); i++) {
                Project p = projects.get(i);
                sb.append(i + 1).append(". ").append(p.getTitle());
                if (p.getSummary() != null && !p.getSummary().isEmpty()) {
                    sb.append(" — ").append(p.getSummary());
                }
                if (p.getTags() != null && !p.getTags().isEmpty()) {
                    sb.append("（").append(p.getTags()).append("）");
                }
                sb.append("\n");
            }
        }

        List<Skill> skills = skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getSortOrder));
        if (!skills.isEmpty()) {
            sb.append("\n## 技能列表\n");
            for (Skill s : skills) {
                sb.append("- ").append(s.getName());
                if (s.getCategory() != null && !s.getCategory().isEmpty()) {
                    sb.append("（").append(s.getCategory()).append("）");
                }
                if (s.getLevel() != null) {
                    sb.append(" ").append(s.getLevel()).append("%");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /** Merge RAG results with catalog. Catalog always included. */
    private String mergeContext(String ragContext, String catalog) {
        if (ragContext == null || ragContext.isEmpty()) return catalog;
        if (catalog.isEmpty()) return ragContext;
        return ragContext + "\n\n" + catalog;
    }

    private String buildSystemPrompt(String context) {
        String base = "你是 Xinki 的个人 AI 助手，代表一位全栈开发者与访客交流。" +
                "你只能回答与作者本人（Xinki）相关的问题，包括：个人简介、技能、经历、作品集、项目详情、联系方式。" +
                "请用中文回答，风格简洁友好，有水墨人文气息。";
        String bounds = "\n\n重要规则：" +
                "\n- 如果访客问到与作者无关的话题（如技术咨询、编程帮助、闲聊、管理后台功能等），请礼貌表示你只能回答关于作者的问题，并引导对方了解作品或联系作者。" +
                "\n- 绝不要提及管理后台、CRUD、API、数据库、技术栈、部署运维等技术实现细节。" +
                "\n- 如果访客想联系作者，引导查看「联系方式」页面。";
        return base + bounds +
                (context != null ? "\n\n以下是你可能需要的背景信息，请基于这些信息回答：\n" + context : "");
    }

    // ==================== 历史记录 ====================

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

    private record ScoredChunk(KnowledgeBase kb, double score) {}
}