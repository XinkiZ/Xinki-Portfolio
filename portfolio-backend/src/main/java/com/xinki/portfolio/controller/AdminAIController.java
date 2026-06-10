package com.xinki.portfolio.controller;

import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.dto.DocumentAnalysisDTO;
import com.xinki.portfolio.service.AdminAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAIController {

    private final AdminAIService adminAIService;

    @PostMapping("/analyze-document")
    public Result<DocumentAnalysisDTO> analyzeDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "鏂囦欢涓嶈兘涓虹┖");
        }
        DocumentAnalysisDTO result = adminAIService.analyzeDocument(file);
        return Result.success(result);
    }
}
