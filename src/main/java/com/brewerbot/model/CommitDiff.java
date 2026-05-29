package com.brewerbot.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommitDiff {
    private String repoName;
    private String sha;
    private String commitMessage;
    private String authorName;
    private List<FileDiff> files;

    @Data
    @Builder
    public static class FileDiff {
        private String filename;
        private String patch;
        private int additions;
        private int deletions;
    }
}
