package com.utkarsh.ai_code_review_assistant.service;

import com.utkarsh.ai_code_review_assistant.dto.GitHubFileDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GitHubService {

    private final RestClient restClient;

    public GitHubService(
            @Value("${github.token}") String githubToken) {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + githubToken)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json")
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2026-03-10")
                .build();
    }

    public List<GitHubFileDTO> getPullRequestFiles(
            String owner,
            String repository,
            int pullRequestNumber) {

        return restClient.get()
                .uri(
                        "/repos/{owner}/{repo}/pulls/{number}/files",
                        owner,
                        repository,
                        pullRequestNumber)
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<GitHubFileDTO>>() {
                        }
                );
    }

    public void createPullRequestComment(
            String owner,
            String repository,
            int pullRequestNumber,
            String review) {

        Map<String, String> requestBody = Map.of(
                "body",
                "## 🤖 AI Code Review\n\n" + review
        );

        restClient.post()
                .uri(
                        "/repos/{owner}/{repo}/issues/{issueNumber}/comments",
                        owner,
                        repository,
                        pullRequestNumber)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        System.out.println(
                "===== AI REVIEW POSTED TO GITHUB ====="
        );
    }
}