package com.xinki.portfolio.service;

import com.xinki.portfolio.dto.GenerateContentResponse;

public interface AIContentService {
    GenerateContentResponse generateProjectContent(String filename, byte[] fileBytes);
}