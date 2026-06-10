package com.xinki.portfolio.service;

import com.xinki.portfolio.dto.DocumentAnalysisDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AdminAIService {
    DocumentAnalysisDTO analyzeDocument(MultipartFile file);
}
