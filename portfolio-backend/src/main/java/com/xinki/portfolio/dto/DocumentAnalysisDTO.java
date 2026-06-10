package com.xinki.portfolio.dto;

import lombok.Data;
import java.util.List;

@Data
public class DocumentAnalysisDTO {
    private ExtractedProject project;
    private List<ExtractedSkill> skills;

    @Data
    public static class ExtractedProject {
        private String title;
        private String description;
        private String tags;
        private String demoUrl;
        private String githubUrl;
    }

    @Data
    public static class ExtractedSkill {
        private String name;
        private String category;
        private Integer level;
    }
}
