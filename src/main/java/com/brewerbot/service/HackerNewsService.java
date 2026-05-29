package com.brewerbot.service;

import com.brewerbot.model.NewsItem;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HackerNewsService {

    private static final int TOP_STORIES_TO_SCAN = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient hnClient;

    public HackerNewsService(@Qualifier("hnWebClient") WebClient hnClient) {
        this.hnClient = hnClient;
    }

    public List<NewsItem> fetchStoriesByKeywords(List<String> keywords, int maxResults) {
        List<Integer> topIds;
        try {
            topIds = hnClient.get()
                .uri("/topstories.json")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Integer>>() {})
                .timeout(TIMEOUT)
                .block();
        } catch (Exception e) {
            log.error("Failed to fetch HN top stories: {}", e.getMessage());
            return List.of();
        }

        if (topIds == null || topIds.isEmpty()) {
            return List.of();
        }

        List<NewsItem> matching = new ArrayList<>();
        for (int id : topIds.subList(0, Math.min(topIds.size(), TOP_STORIES_TO_SCAN))) {
            if (matching.size() >= maxResults * 3) {
                break;
            }
            try {
                JsonNode item = hnClient.get()
                    .uri("/item/{id}.json", id)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(TIMEOUT)
                    .block();

                if (item == null || !"story".equals(item.path("type").asText())) {
                    continue;
                }

                String title = item.path("title").asText();
                if (title.isBlank()) {
                    continue;
                }

                boolean matches = keywords.stream()
                    .anyMatch(kw -> title.toLowerCase().contains(kw.toLowerCase()));
                if (!matches) {
                    continue;
                }

                String url = item.path("url").asText();
                if (url.isBlank()) {
                    url = "https://news.ycombinator.com/item?id=" + id;
                }

                matching.add(NewsItem.builder()
                    .title(title)
                    .url(url)
                    .score(item.path("score").asInt())
                    .source("Hacker News")
                    .build());

            } catch (Exception e) {
                log.debug("Failed to fetch HN item {}: {}", id, e.getMessage());
            }
        }

        return matching.stream()
            .sorted(Comparator.comparingInt(NewsItem::getScore).reversed())
            .limit(maxResults)
            .collect(Collectors.toList());
    }
}
