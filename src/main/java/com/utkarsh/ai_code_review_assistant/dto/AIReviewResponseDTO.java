package com.utkarsh.ai_code_review_assistant.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIReviewResponseDTO {

    private String summary;
    private List<Issue> issues;
    private List<String> suggestions;

    @Data
    public static class Issue {
        private String severity;
        private String file;
        private Integer line;
        private String description;
    }
}