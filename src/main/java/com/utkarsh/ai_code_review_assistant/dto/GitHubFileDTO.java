package com.utkarsh.ai_code_review_assistant.dto;

import lombok.Data;

@Data
public class GitHubFileDTO {

    private String sha;
    private String filename;
    private String status;
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private String blob_url;
    private String raw_url;
    private String contents_url;
    private String patch;
}