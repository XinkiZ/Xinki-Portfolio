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
        String systemPrompt = "你是一个专业的技术项目分析助手。\n" +
                "用户会上传一个文档（如PDF或README），你需要从中提取以下信息：\n" +
                "1. 项目信息（标题、描述、技术标签、演示链接、源码链接）\n" +
                "2. 技能列表：从文档中识别出的技术栈名称、分类、熟练度\n\n" +
                "请严格以以下 JSON 格式输出（仅输出JSON，不要包含任何其他文本）：\n" +
                "{\n" +
                "  \"project\": {\n" +
                "    \"title\": \"项目名称\",\n" +
                "    \"description\": \"项目描述，尽量详细（200字内）\",\n" +
                "    \"tags\": \"Spring Boot, Vue, MySQL\",\n" +
                "    \"demoUrl\": \"演示链接，无则为空\",\n" +
                "    \"githubUrl\": \"源码链接，无则为空\"\n" +
                "  },\n" +
                "  \"skills\": [\n" +
                "    { \"name\": \"Spring Boot\", \"category\": \"后端\", \"level\": 85 },\n" +
                "    { \"name\": \"Vue.js\", \"category\": \"前端\", \"level\": 80 }\n" +
                "  ]\n" +
                "}\n\n" +
                "如果无法确定某个字段，请使用空字符串或 0 作为默认值。\n" +
                "技能熟练度（level）范围 1-100，根据文档中描述的深度评估。\n" +
                "tags 字段为逗号分隔的技术标签字符串。";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content",
                "文档名称：" + filename + "\n\n文档内容：\n" + documentContent));

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "messages", messages,
                "temperature", 0.3,
                "max_tokens", 2000
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