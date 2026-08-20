package com.utkarsh.ai_code_review_assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utkarsh.ai_code_review_assistant.dto.AIReviewResponseDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIReviewService {

    private final RestClient restClient;
    private final String geminiApiKey;
    private final ObjectMapper objectMapper;

    public AIReviewService(
            @Value("${gemini.api-key}") String geminiApiKey) {

        this.geminiApiKey = geminiApiKey;
        this.objectMapper = new ObjectMapper();

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(
                        "x-goog-api-key",
                        geminiApiKey
                )
                .defaultHeader(
                        "Content-Type",
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public String reviewCode(String code) {

        String prompt = """
                You are an expert Java code reviewer.

                Review the following Java code and identify:

                1. Bugs
                2. Runtime exceptions
                3. Null pointer risks
                4. Security issues
                5. Performance issues
                6. Code quality problems
                7. Suggested improvements

                Return ONLY valid JSON.

                Do NOT return Markdown.
                Do NOT use ```json.
                Do NOT add any explanation outside the JSON.

                Return exactly this structure:

                {
                  "summary": "Short summary of the overall code review",
                  "issues": [
                    {
                      "severity": "HIGH",
                      "file": "TestCode.java",
                      "line": 7,
                      "description": "Detailed explanation of the issue"
                    }
                  ],
                  "suggestions": [
                    "Suggestion for improving the code"
                  ]
                }

                Severity must be exactly one of:

                LOW
                MEDIUM
                HIGH
                CRITICAL

                If there are no issues, return:

                "issues": []

                If there are no suggestions, return:

                "suggestions": []

                If the exact line number is unknown, use:

                "line": null

                Code to review:

                %s
                """.formatted(code);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        for (int attempt = 1; attempt <= 3; attempt++) {

            try {

                String response = restClient.post()
                        .uri("/v1beta/models/gemini-3.5-flash:generateContent")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                return extractAndFormatReview(response);

            } catch (
                    org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {

                System.out.println(
                        "Gemini is temporarily unavailable. Attempt "
                                + attempt + " of 3"
                );

                if (attempt == 3) {

                    return "AI review temporarily unavailable. Please try again later.";
                }

                try {

                    Thread.sleep(2000);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    return "AI review interrupted.";
                }

            } catch (Exception e) {

                System.out.println(
                        "Failed to generate AI review: "
                                + e.getMessage()
                );

                return "Failed to generate AI review: "
                        + e.getMessage();
            }
        }

        return "AI review unavailable.";
    }

    private String extractAndFormatReview(String response) {

        try {

            /*
             * Gemini's API response is converted
             * into a Map.
             *
             * No JsonNode is used.
             */
            Map<String, Object> root =
                    objectMapper.readValue(
                            response,
                            new TypeReference<Map<String, Object>>() {
                            }
                    );

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>)
                            root.get("candidates");

            if (candidates == null
                    || candidates.isEmpty()) {

                return "Gemini returned no review.";
            }

            Map<String, Object> candidate =
                    candidates.get(0);

            Map<String, Object> candidateContent =
                    (Map<String, Object>)
                            candidate.get("content");

            if (candidateContent == null) {

                return "Gemini returned no review content.";
            }

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>)
                            candidateContent.get("parts");

            if (parts == null
                    || parts.isEmpty()) {

                return "Gemini returned no review content.";
            }

            String reviewJson =
                    (String) parts.get(0).get("text");

            if (reviewJson == null
                    || reviewJson.isBlank()) {

                return "Gemini returned an empty review.";
            }

            reviewJson =
                    cleanJsonResponse(reviewJson);

            /*
             * Convert Gemini's generated JSON
             * into our DTO.
             */
            AIReviewResponseDTO review =
                    objectMapper.readValue(
                            reviewJson,
                            AIReviewResponseDTO.class
                    );

            /*
             * Convert DTO to String because
             * the existing controller expects String.
             */
            return formatReview(review);

        } catch (Exception e) {

            System.out.println(
                    "Failed to parse Gemini review: "
                            + e.getMessage()
            );

            return "Failed to parse Gemini review: "
                    + e.getMessage();
        }
    }

    private String cleanJsonResponse(String reviewJson) {

        reviewJson = reviewJson.trim();

        if (reviewJson.startsWith("```json")) {

            reviewJson = reviewJson
                    .substring(7)
                    .trim();

        } else if (reviewJson.startsWith("```")) {

            reviewJson = reviewJson
                    .substring(3)
                    .trim();
        }

        if (reviewJson.endsWith("```")) {

            reviewJson = reviewJson
                    .substring(
                            0,
                            reviewJson.length() - 3
                    )
                    .trim();
        }

        return reviewJson;
    }

    private String formatReview(
            AIReviewResponseDTO review) {

        StringBuilder result =
                new StringBuilder();

        result.append("## 🤖 AI Code Review\n\n");

        result.append("### Summary\n\n");

        if (review.getSummary() != null
                && !review.getSummary().isBlank()) {

            result.append(
                    review.getSummary()
            );

        } else {

            result.append(
                    "No summary provided."
            );
        }

        result.append("\n\n");

        result.append("### Issues\n\n");

        if (review.getIssues() != null
                && !review.getIssues().isEmpty()) {

            for (AIReviewResponseDTO.Issue issue
                    : review.getIssues()) {

                result.append("- **");

                if (issue.getSeverity() != null) {

                    result.append(
                            issue.getSeverity()
                    );

                } else {

                    result.append("UNKNOWN");
                }

                result.append("**");

                if (issue.getFile() != null) {

                    result.append(" `")
                            .append(issue.getFile())
                            .append("`");
                }

                if (issue.getLine() != null) {

                    result.append(" - Line ")
                            .append(issue.getLine());
                }

                result.append("\n");

                if (issue.getDescription() != null) {

                    result.append("  ")
                            .append(issue.getDescription());
                }

                result.append("\n\n");
            }

        } else {

            result.append(
                    "No major issues found. ✅\n\n"
            );
        }

        if (review.getSuggestions() != null
                && !review.getSuggestions().isEmpty()) {

            result.append(
                    "### Suggestions\n\n"
            );

            for (String suggestion
                    : review.getSuggestions()) {

                result.append("- ")
                        .append(suggestion)
                        .append("\n");
            }
        }

        return result.toString();
    }
}