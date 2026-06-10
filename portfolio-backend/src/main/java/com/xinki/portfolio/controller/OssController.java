package com.xinki.portfolio.controller;

import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    @PostMapping
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        String url = ossService.upload(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }
}