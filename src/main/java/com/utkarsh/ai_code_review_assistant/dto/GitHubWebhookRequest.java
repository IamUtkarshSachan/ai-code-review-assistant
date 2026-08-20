package com.utkarsh.ai_code_review_assistant.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubWebhookRequest {

    private String action;
    private Integer number;
    private PullRequest pull_request;
    private Repository repository;

    @Getter
    @Setter
    public static class PullRequest {

        private String title;
        private String html_url;
    }

    @Getter
    @Setter
    public static class Repository {

        private String name;
        private Owner owner;
    }

    @Getter
    @Setter
    public static class Owner {

        private String login;
    }
}