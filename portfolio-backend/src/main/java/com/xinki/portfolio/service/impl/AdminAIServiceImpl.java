package com.xinki.portfolio.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinki.portfolio.dto.DocumentAnalysisDTO;
import com.xinki.portfolio.dto.DocumentAnalysisDTO.ExtractedProject;
import com.xinki.portfolio.dto.DocumentAnalysisDTO.ExtractedSkill;
import com.xinki.portfolio.service.AdminAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAIServiceImpl implements AdminAIService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DocumentAnalysisDTO analyzeDocument(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            byte[] bytes = file.getBytes();
            String text = extractText(filename, bytes);
            if (text == null || text.trim().isEmpty()) {
                return emptyResult();
            }

            if (text.length() > 8000) {
                text = text.substring(0, 8000);
            }

            String prompt = "请分析以下文档内容，提取项目信息和技能标签。\n" +
                    "输出JSON格式：\n" +
                    "{\n" +
                    "  \"project\": {\n" +
                    "    \"title\": \"项目名称\",\n" +
                    "    \"summary\": \"简短描述\",\n" +
                    "    \"description\": \"详细HTML描述\",\n" +
                    "    \"tags\": \"标签（逗号分隔）\",\n" +
                    "    \"demoUrl\": \"\",\n" +
                    "    \"githubUrl\": \"\"\n" +
                    "  },\n" +
                    "  \"skills\": [\n" +
                    "    {\"name\": \"技能名\", \"category\": \"分类\", \"level\": 80}\n" +
                    "  ]\n" +
                    "}\n\n" +
                    "文档内容：\n" + text;

            String response = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseResponse(response);
        } catch (Exception e) {
            log.error("Document analysis failed", e);
            return emptyResult();
        }
    }

    private String extractText(String filename, byte[] bytes) {
        try {
            if (filename == null) return "";
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) {
                try (PDDocument doc = PDDocument.load(new java.io.ByteArrayInputStream(bytes))) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    return stripper.getText(doc);
                }
            } else if (lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return "";
        } catch (Exception e) {
            log.error("Text extraction failed", e);
            return "";
        }
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