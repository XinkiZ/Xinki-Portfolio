package com.xinki.portfolio.controller;

import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.dto.ChatRequestDTO;
import com.xinki.portfolio.service.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

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
}
