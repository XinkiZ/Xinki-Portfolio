package com.xinki.portfolio.controller;

import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.dto.ChatRequestDTO;
import com.xinki.portfolio.service.AIChatService;
import com.xinki.portfolio.service.AIContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;
    private final AIContentService aiContentService;

    @PostMapping
    public Result<?> chat(@RequestBody ChatRequestDTO request) {
        return Result.success(aiChatService.chat(request.getSessionId(), request.getMessage()));
    }

    @GetMapping("/{sessionId}")
    public Result<?> history(@PathVariable String sessionId) {
        return Result.success(aiChatService.getHistory(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public Result<?> clear(@PathVariable String sessionId) {
        aiChatService.clearHistory(sessionId);
        return Result.success("已清除");
    }

    @PostMapping("/generate-content")
    public Result<?> generateProjectContent(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        try {
            String filename = file.getOriginalFilename();
            byte[] bytes = file.getBytes();
            return Result.success(aiContentService.generateProjectContent(filename, bytes));
        } catch (Exception e) {
            return Result.error(500, "文件处理失败：" + e.getMessage());
        }
    }
}