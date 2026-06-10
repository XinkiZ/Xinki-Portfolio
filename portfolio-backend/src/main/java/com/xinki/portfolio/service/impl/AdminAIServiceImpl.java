package com.xinki.portfolio.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinki.portfolio.config.AIConfig;
import com.xinki.portfolio.dto.DocumentAnalysisDTO;
import com.xinki.portfolio.dto.DocumentAnalysisDTO.ExtractedProject;
import com.xinki.portfolio.dto.DocumentAnalysisDTO.ExtractedSkill;
import com.xinki.portfolio.service.AdminAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAIServiceImpl implements AdminAIService {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public DocumentAnalysisDTO analyzeDocument(MultipartFile file) {
        try {
            String content = extractText(file);
            if (content == null || content.isBlank()) {
                return emptyResult();
            }
            if (content.length() > 8000) {
                content = content.substring(0, 8000);
            }
            String jsonResponse = callBailianForExtraction(content, file.getOriginalFilename());
            return parseResponse(jsonResponse);
        } catch (Exception e) {
            log.error("Document analysis failed", e);
            return emptyResult();
        }
    }

    private String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(doc);
            }
        }
        return new String(file.getBytes(), "UTF-8");
    }

    private String callBailianForExtraction(String documentContent, String filename) throws Exception {
        String systemPrompt = "你是一名资深技术文案编辑。阅读用户上传的项目文档，提取以下信息：\n\n" +
                "## 1. 项目信息\n" +
                "- title：项目名称\n" +
                "- summary：一句话简介（50-120字），必须包含核心亮点和数据（如性能提升、用户规模）\n" +
                "  忌空话堆砌（如\"功能强大、体验优秀\"）\n" +
                "- description：详情页展示内容。自主判定复杂度：\n" +
                "  简单项目 → <p> 段落；复杂项目 → 用 <h3> <ul><li> <ol><li> <pre><code> <blockquote> <strong> 组织\n" +
                "  规则：禁止 <html><head><body>；代码块保留缩进；信息密度优先\n" +
                "- tags：逗号分隔的技术标签\n" +
                "- demoUrl / githubUrl：从文档提取，无则为空\n\n" +
                "## description 质量参考\n" +
                "好的输出：\n" +
                "<h3>项目概述</h3>\n<p>苍穹外卖是面向中小型餐饮商家的全栈外卖点餐解决方案，覆盖用户端、商家端、管理后台三大场景。</p>\n<h3>技术架构</h3>\n<ul><li><strong>后端：</strong>Spring Boot 2.7 + MyBatis-Plus + Redis + WebSocket</li><li><strong>前端：</strong>Vue 3 + TypeScript + Pinia + Element Plus</li></ul>\n<h3>核心功能</h3>\n<ol><li>实时订单推送 — WebSocket 毫秒级状态同步</li><li>智能缓存 — Redis 缓存热门数据，响应时间降低 60%</li></ol>\n<blockquote>独立完成全栈开发，项目周期 3 个月</blockquote>\n\n" +
                "差的输出：<p>这是一个很好的项目，功能非常强大，用了很多技术，非常优秀。</p> ← 信息量为零，禁止\n\n" +
                "## 2. 技能列表\n" +
                "- name：技术名称\n" +
                "- category：分类（后端 / 前端 / 数据库 / 运维 / 工具）\n" +
                "- level：熟练度 1-100\n\n" +
                "严格输出纯 JSON（不要 markdown 包裹）：\n" +
                "{\"project\":{\"title\":\"\",\"summary\":\"\",\"description\":\"\",\"tags\":\"\",\"demoUrl\":\"\",\"githubUrl\":\"\"},\"skills\":[{\"name\":\"\",\"category\":\"\",\"level\":85}]}";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content",
                "文档名称：" + filename + "\n\n文档内容：\n" + documentContent));

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
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private DocumentAnalysisDTO parseResponse(String jsonResponse) {
        try {
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```")) {
                int start = cleaned.indexOf("\n") + 1;
                int end = cleaned.lastIndexOf("```");
                if (end > start) {
                    cleaned = cleaned.substring(start, end).trim();
                }
            }
            JsonNode root = objectMapper.readTree(cleaned);
            DocumentAnalysisDTO result = new DocumentAnalysisDTO();

            if (root.has("project") && !root.get("project").isNull()) {
                JsonNode p = root.get("project");
                ExtractedProject project = new ExtractedProject();
                project.setTitle(p.path("title").asText(""));
                project.setSummary(p.path("summary").asText(""));
                project.setDescription(p.path("description").asText(""));
                project.setTags(p.path("tags").asText(""));
                project.setDemoUrl(p.path("demoUrl").asText(""));
                project.setGithubUrl(p.path("githubUrl").asText(""));
                result.setProject(project);
            }

            if (root.has("skills") && root.get("skills").isArray()) {
                List<ExtractedSkill> skills = new ArrayList<>();
                for (JsonNode s : root.get("skills")) {
                    ExtractedSkill skill = new ExtractedSkill();
                    skill.setName(s.path("name").asText(""));
                    skill.setCategory(s.path("category").asText(""));
                    skill.setLevel(s.path("level").asInt(50));
                    if (!skill.getName().isBlank()) {
                        skills.add(skill);
                    }
                }
                result.setSkills(skills);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonResponse, e);
            return emptyResult();
        }
    }

    private DocumentAnalysisDTO emptyResult() {
        DocumentAnalysisDTO dto = new DocumentAnalysisDTO();
        dto.setProject(new ExtractedProject());
        dto.setSkills(Collections.emptyList());
        return dto;
    }
}