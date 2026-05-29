package com.brewerbot.scheduler;

import com.brewerbot.service.GitHubMonitorService;
import com.brewerbot.service.NewsAggregatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotScheduler {

    private final GitHubMonitorService gitHubMonitor;
    private final NewsAggregatorService newsAggregator;

    @Scheduled(cron = "${app.scheduling.github-check-cron}")
    public void checkGitHub() {
        log.info("=== GitHub check triggered ===");
        gitHubMonitor.checkForNewCommits();
    }

    @Scheduled(cron = "${app.scheduling.java-news-cron}")
    public void postJavaNews() {
        log.info("=== Java/Spring news check triggered ===");
        newsAggregator.checkAndPostJavaNews();
    }

    @Scheduled(cron = "${app.scheduling.ai-news-cron}")
    public void postAiNews() {
        log.info("=== AI news check triggered ===");
        newsAggregator.checkAndPostAiNews();
    }
}
