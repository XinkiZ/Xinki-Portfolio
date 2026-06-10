package com.xinki.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponseDTO {
    private String role;
    private String content;
    private String sessionId;
}
