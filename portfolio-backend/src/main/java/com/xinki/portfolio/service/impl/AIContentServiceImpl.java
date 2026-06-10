package com.xinki.portfolio.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinki.portfolio.config.AIConfig;
import com.xinki.portfolio.dto.GenerateContentResponse;
import com.xinki.portfolio.service.AIContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIContentServiceImpl implements AIContentService {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public GenerateContentResponse generateProjectContent(String filename, byte[] fileBytes) {
        String extractedText = extractText(filename, fileBytes);
        if (extractedText == null || extractedText.isBlank()) {
            GenerateContentResponse resp = new GenerateContentResponse();
            resp.setSummary("无法提取文件内容");
            resp.setDescription("<p>文件内容为空或格式不支持。</p>");
            return resp;
        }

        String textForAI = extractedText.length() > 8000
                ? extractedText.substring(0, 8000) + "\n...(内容过长，已截断)"
                : extractedText;

        return callAI(textForAI, filename);
    }

    private String extractText(String filename, byte[] fileBytes) {
        try {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) {
                return extractPdfText(fileBytes);
            } else if (lower.endsWith(".md") || lower.endsWith(".txt")
                    || lower.endsWith(".markdown") || lower.endsWith(".readme")) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            } else {
                // 尝试作为 UTF-8 文本读取
                return new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Text extraction failed for {}", filename, e);
            return null;
        }
    }

    private String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private GenerateContentResponse callAI(String text, String filename) {
        try {
            String systemPrompt = "你是一名资深技术文案编辑。阅读用户上传的项目文件，生成两部分内容：\n\n" +
                    "## summary（简介）\n" +
                    "- 50-120 字中文简介，用于作品卡片展示\n" +
                    "- 必须包含：解决什么问题、核心技术栈、最亮点的功能或数据\n" +
                    "- 忌空话堆砌（如\"功能强大、体验优秀\"）\n\n" +
                    "## description（详细描述，HTML 格式）\n" +
                    "- 自主判定复杂度：简单项目用 <p> 段落；复杂项目用丰富标签\n" +
                    "- 可用标签：<h3> <p> <ul><li> <ol><li> <pre><code> <blockquote> <strong> <em> <br> <hr>\n" +
                    "- 禁止使用 <html> <head> <body> 标签\n" +
                    "- 代码块保留原始缩进；信息密度优先\n\n" +
                    "## description 质量参考\n" +
                    "好的输出：\n" +
                    "<h3>项目概述</h3>\n<p>苍穹外卖是面向中小型餐饮商家的全栈外卖点餐解决方案，覆盖用户端、商家端、管理后台三大场景。</p>\n<h3>技术架构</h3>\n<ul><li><strong>后端：</strong>Spring Boot 2.7 + MyBatis-Plus + Redis + WebSocket</li><li><strong>前端：</strong>Vue 3 + TypeScript + Pinia + Element Plus</li></ul>\n<h3>核心功能</h3>\n<ol><li>实时订单推送 — WebSocket 毫秒级状态同步</li><li>智能缓存 — Redis 缓存热门数据，响应时间降低 60%</li></ol>\n<blockquote>独立完成全栈开发，项目周期 3 个月</blockquote>\n\n" +
                    "差的输出：<p>这是一个很好的项目，功能非常强大，用了很多技术，非常优秀。</p> ← 信息量为零，禁止\n\n" +
                    "严格输出以下 JSON（不要 markdown 包裹）：\n" +
                    "{\"summary\":\"简介内容\",\"description\":\"HTML内容\"}";

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", "文件名：" + filename + "\n\n文件内容：\n" + text)
            );

            Map<String, Object> body = Map.of(
                    "model", aiConfig.getModel(),
                    "messages", messages,
                    "temperature", 0.3,
                    "max_tokens", 4096
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String aiOutput = root.path("choices").get(0).path("message").path("content").asText();

            String jsonStr = aiOutput.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            JsonNode resultNode = objectMapper.readTree(jsonStr);

            GenerateContentResponse resp = new GenerateContentResponse();
            resp.setSummary(resultNode.path("summary").asText(""));
            resp.setDescription(resultNode.path("description").asText(""));
            return resp;

        } catch (Exception e) {
            log.error("AI content generation failed", e);
            GenerateContentResponse resp = new GenerateContentResponse();
            resp.setSummary("生成失败");
            resp.setDescription("<p>AI 生成内容时出错，请稍后重试。</p>");
            return resp;
        }
    }
}