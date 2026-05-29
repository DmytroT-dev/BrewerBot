package com.brewerbot.service;

import com.brewerbot.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsAggregatorService {

    private final HackerNewsService hackerNewsService;
    private final RedditService redditService;
    private final ContentGeneratorService contentGenerator;
    private final TelegramPublisherService telegramPublisher;
    private final StateManager stateManager;

    private static final List<String> JAVA_HN_KEYWORDS = List.of(
        "java", "spring", "jvm", "kotlin", "quarkus", "micronaut",
        "hibernate", "jakarta", "graalvm", "maven", "gradle", "jdk"
    );

    private static final List<String> JAVA_SUBREDDITS = List.of(
        "java", "SpringBoot", "javahelp", "learnjava", "Kotlin"
    );

    private static final List<String> AI_HN_KEYWORDS = List.of(
        "gpt", "llm", "claude", "gemini", "openai", "anthropic",
        "mistral", "llama", "chatgpt", "deepmind", "neural network",
        "large language model", "diffusion", "ai model", "foundation model"
    );

    private static final List<String> AI_SUBREDDITS = List.of(
        "MachineLearning", "artificial", "LocalLLaMA", "singularity",
        "OpenAI", "ClaudeAI", "ChatGPT"
    );

    private static final int POSTS_TO_SELECT = 1;

    public void checkAndPostJavaNews() {
        log.info("Fetching Java/Spring news...");
        stateManager.updateLastJavaNewsCheck();

        List<NewsItem> all = new ArrayList<>();
        all.addAll(hackerNewsService.fetchStoriesByKeywords(JAVA_HN_KEYWORDS, 15));
        all.addAll(redditService.fetchTopPosts(JAVA_SUBREDDITS, 5));

        publishTopNews(all, "java-spring");
    }

    public void checkAndPostAiNews() {
        log.info("Fetching AI news...");
        stateManager.updateLastAiNewsCheck();

        List<NewsItem> all = new ArrayList<>();
        all.addAll(hackerNewsService.fetchStoriesByKeywords(AI_HN_KEYWORDS, 20));
        all.addAll(redditService.fetchTopPosts(AI_SUBREDDITS, 5));

        publishTopNews(all, "ai");
    }

    private void publishTopNews(List<NewsItem> all, String category) {
        List<NewsItem> unposted = all.stream()
            .filter(item -> !stateManager.isNewsAlreadyPosted(item.getUrl()))
            .toList();

        if (unposted.isEmpty()) {
            log.info("No new {} news to post", category);
            return;
        }

        log.info("Found {} new {} items, selecting top {}", unposted.size(), category, POSTS_TO_SELECT);

        List<NewsItem> selected = contentGenerator.rankAndSelectNews(unposted, category, POSTS_TO_SELECT);

        for (NewsItem item : selected) {
            contentGenerator.generateNewsPost(item, category).ifPresentOrElse(
                post -> {
                    telegramPublisher.sendMessage(post);
                    stateManager.markNewsAsPosted(item.getUrl());
                    log.info("Posted [{}]: {}", category, item.getTitle());
                    sleepSilently(2500);
                },
                () -> log.warn("Claude returned empty post for: {}", item.getTitle())
            );
        }
    }

    private void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
