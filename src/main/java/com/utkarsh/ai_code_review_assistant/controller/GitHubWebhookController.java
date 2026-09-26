package com.utkarsh.ai_code_review_assistant.controller;

import com.utkarsh.ai_code_review_assistant.dto.GitHubFileDTO;
import com.utkarsh.ai_code_review_assistant.dto.GitHubWebhookRequest;
import com.utkarsh.ai_code_review_assistant.service.AIReviewService;
import com.utkarsh.ai_code_review_assistant.service.GitHubService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/github")
public class GitHubWebhookController {

    private final GitHubService gitHubService;
    private final AIReviewService aiReviewService;

    public GitHubWebhookController(
            GitHubService gitHubService,
            AIReviewService aiReviewService) {

        this.gitHubService = gitHubService;
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(
            @RequestBody GitHubWebhookRequest payload,
            @RequestHeader(
                    value = "X-GitHub-Event",
                    required = false
            ) String event) {

        System.out.println(
                "===== GITHUB WEBHOOK RECEIVED ====="
        );

        System.out.println("Event: " + event);
        System.out.println("Action: " + payload.getAction());
        System.out.println("PR Number: " + payload.getNumber());

        /*
         * Process only Pull Request actions
         * that can contain changed code.
         */
        if (!"opened".equals(payload.getAction())
                && !"synchronize".equals(payload.getAction())
                && !"reopened".equals(payload.getAction())) {

            System.out.println(
                    "Event ignored: "
                            + payload.getAction()
            );

            return ResponseEntity.ok(
                    "Event ignored"
            );
        }

        /*
         * Extract repository information
         * from the webhook payload.
         */
        String owner = payload
                .getRepository()
                .getOwner()
                .getLogin();

        String repository = payload
                .getRepository()
                .getName();

        Integer prNumber =
                payload.getNumber();

        System.out.println(
                "Repository: "
                        + owner
                        + "/"
                        + repository
        );

        System.out.println(
                "PR Number: "
                        + prNumber
        );

        /*
         * Get all files changed in the Pull Request.
         */
        List<GitHubFileDTO> files =
                gitHubService.getPullRequestFiles(
                        owner,
                        repository,
                        prNumber
                );

        System.out.println(
                "===== CHANGED FILES ====="
        );

        /*
         * Review changed Java files only.
         */
        for (GitHubFileDTO file : files) {

            System.out.println(
                    "File: "
                            + file.getFilename()
            );

            /*
             * Ignore non-Java files.
             */
            if (!file.getFilename().endsWith(".java")) {

                System.out.println(
                        "Skipping non-Java file: "
                                + file.getFilename()
                );

                continue;
            }

            System.out.println(
                    "Status: "
                            + file.getStatus()
            );

            System.out.println(
                    "Additions: "
                            + file.getAdditions()
            );

            System.out.println(
                    "Deletions: "
                            + file.getDeletions()
            );

            /*
             * A patch contains the actual
             * changed lines of the file.
             */
            String patch =
                    file.getPatch();

            System.out.println(
                    "===== PATCH ====="
            );

            System.out.println(patch);

            /*
             * Some GitHub files do not have
             * a patch, for example binary files.
             */
            if (patch == null
                    || patch.isBlank()) {

                System.out.println(
                        "No patch available. Skipping."
                );

                continue;
            }

            /*
             * Send the changed Java code to Gemini.
             */
            System.out.println(
                    "===== AI CODE REVIEW ====="
            );

            String review =
                    aiReviewService.reviewCode(
                            patch
                    );

            System.out.println(review);

            System.out.println(
                    "===== END AI CODE REVIEW ====="
            );

            /*
             * Post the generated review
             * back to the Pull Request.
             */
            gitHubService.createPullRequestComment(
                    owner,
                    repository,
                    prNumber,
                    review
            );

            System.out.println(
                    "AI review posted to GitHub."
            );
        }

        return ResponseEntity.ok(
                "Webhook processed successfully"
        );
    }
}