package com.xinki.portfolio.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinki.portfolio.dto.GenerateContentResponse;
import com.xinki.portfolio.service.AIContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIContentServiceImpl implements AIContentService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GenerateContentResponse generateProjectContent(String filename, byte[] fileBytes) {
        String text = extractText(filename, fileBytes);
        if (text == null || text.trim().isEmpty()) {
            text = "无法提取文本内容";
        }

        String prompt = "请基于以下文档内容，生成作品简介和HTML描述。\n" +
                "要求：\n" +
                "1. 简介不超过200字\n" +
                "2. HTML描述使用语义化标签（h2, p, ul, code等），适合展示\n" +
                "3. 输出JSON格式：{\"summary\": \"简介\", \"description\": \"HTML描述\"}\n\n" +
                "文档内容：\n" + text;

        String response = chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .call()
                .content();

        return parseResponse(response);
    }

    private String extractText(String filename, byte[] fileBytes) {
        try {
            if (filename == null) return "";
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) {
                try (PDDocument doc = PDDocument.load(new java.io.ByteArrayInputStream(fileBytes))) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    return stripper.getText(doc);
                }
            } else if (lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            }
            return "";
        } catch (Exception e) {
            log.error("Failed to extract text from {}", filename, e);
            return "";
        }
    }

    private GenerateContentResponse parseResponse(String jsonResponse) {
        try {
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```")) {
                int start = cleaned.indexOf("\n") + 1;
                int end = cleaned.lastIndexOf("```");
                if (end > start) {
                    cleaned = cleaned.substring(start, end).trim();
                }
            }
            return objectMapper.readValue(cleaned, GenerateContentResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonResponse, e);
            GenerateContentResponse fallback = new GenerateContentResponse();
            fallback.setSummary("无法生成简介");
            fallback.setDescription("<p>内容生成失败，请稍后重试。</p>");
            return fallback;
        }
    }
}