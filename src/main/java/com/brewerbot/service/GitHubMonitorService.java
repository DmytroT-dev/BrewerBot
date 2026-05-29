package com.brewerbot.service;

import com.brewerbot.config.AppProperties;
import com.brewerbot.model.CommitDiff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubMonitorService {

    private final AppProperties props;
    private final StateManager stateManager;
    private final ContentGeneratorService contentGenerator;
    private final TelegramPublisherService telegramPublisher;

    private static final int MAX_PATCH_LENGTH = 3000;
    private static final int MAX_FILES_PER_COMMIT = 3;
    private static final int MAX_NEW_COMMITS = 3;

    public void checkForNewCommits() {
        try {
            GitHub gitHub = new GitHubBuilder()
                .withOAuthToken(props.getGithub().getToken())
                .build();

            GHUser user = gitHub.getUser(props.getGithub().getUsername());
            Map<String, GHRepository> repos = user.getRepositories();
            List<String> targetRepos = props.getGithub().getRepos();

            for (GHRepository repo : repos.values()) {
                if (!targetRepos.isEmpty() && !targetRepos.contains(repo.getName())) {
                    continue;
                }
                if (repo.isArchived() || repo.isFork()) {
                    continue;
                }
                try {
                    processRepository(repo);
                } catch (Exception e) {
                    log.warn("Skipping repo {}: {}", repo.getName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("GitHub check failed: {}", e.getMessage());
        }
    }

    private void processRepository(GHRepository repo) throws IOException {
        String repoFullName = repo.getFullName();
        String lastSha = stateManager.getLastCommitSha(repoFullName);

        List<GHCommit> newCommits = new ArrayList<>();
        String latestSha = null;

        for (GHCommit commit : repo.queryCommits().pageSize(20).list()) {
            String sha = commit.getSHA1();
            if (latestSha == null) {
                latestSha = sha;
            }
            if (sha.equals(lastSha)) {
                break;
            }
            newCommits.add(commit);
            if (newCommits.size() >= MAX_NEW_COMMITS) {
                break;
            }
        }

        if (latestSha == null) {
            return;
        }

        // First time seeing this repo — just store the SHA, don't post
        if (lastSha == null) {
            stateManager.updateLastCommitSha(repoFullName, latestSha);
            log.info("Bootstrapped repo {}, stored SHA {}", repoFullName, latestSha.substring(0, 7));
            return;
        }

        if (newCommits.isEmpty()) {
            return;
        }

        stateManager.updateLastCommitSha(repoFullName, latestSha);

        for (GHCommit commit : newCommits) {
            try {
                processCommit(repo, commit);
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Failed to process commit {}: {}", commit.getSHA1().substring(0, 7), e.getMessage());
            }
        }
    }

    private void processCommit(GHRepository repo, GHCommit commit) throws IOException {
        List<CommitDiff.FileDiff> javaDiffs = new ArrayList<>();

        for (GHCommit.File file : commit.getFiles()) {
            if (!file.getFileName().endsWith(".java")) {
                continue;
            }
            String patch = file.getPatch();
            if (patch == null || patch.isBlank()) {
                continue;
            }
            if (patch.length() > MAX_PATCH_LENGTH) {
                patch = patch.substring(0, MAX_PATCH_LENGTH) + "\n... (truncated)";
            }
            javaDiffs.add(CommitDiff.FileDiff.builder()
                .filename(file.getFileName())
                .patch(patch)
                .additions(file.getLinesAdded())
                .deletions(file.getLinesDeleted())
                .build());

            if (javaDiffs.size() >= MAX_FILES_PER_COMMIT) {
                break;
            }
        }

        if (javaDiffs.isEmpty()) {
            return;
        }

        GHCommit.ShortInfo info = commit.getCommitShortInfo();
        CommitDiff diff = CommitDiff.builder()
            .repoName(repo.getName())
            .sha(commit.getSHA1().substring(0, 7))
            .commitMessage(info.getMessage())
            .authorName(info.getAuthor().getName())
            .files(javaDiffs)
            .build();

        log.info("Analysing commit {}/{}", diff.getRepoName(), diff.getSha());

        contentGenerator.generateCodePost(diff).ifPresent(post -> {
            telegramPublisher.sendMessage(post);
            log.info("Posted code insight for {}/{}", diff.getRepoName(), diff.getSha());
        });
    }
}
