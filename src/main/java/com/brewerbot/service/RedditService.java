package com.brewerbot.service;

import com.brewerbot.model.NewsItem;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class RedditService {

    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    private final WebClient redditClient;

    public RedditService(@Qualifier("redditWebClient") WebClient redditClient) {
        this.redditClient = redditClient;
    }

    public List<NewsItem> fetchTopPosts(List<String> subreddits, int postsPerSubreddit) {
        List<NewsItem> items = new ArrayList<>();

        for (String subreddit : subreddits) {
            try {
                JsonNode response = redditClient.get()
                    .uri("/r/{sub}/hot.json?limit={limit}&t=day", subreddit, postsPerSubreddit)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(TIMEOUT)
                    .block();

                if (response == null) {
                    continue;
                }

                JsonNode children = response.path("data").path("children");
                for (JsonNode post : children) {
                    JsonNode data = post.path("data");

                    String title = data.path("title").asText();
                    if (title.isBlank()) {
                        continue;
                    }

                    // Skip stickied mod posts
                    if (data.path("stickied").asBoolean()) {
                        continue;
                    }

                    boolean isSelf = data.path("is_self").asBoolean();
                    String url;
                    if (isSelf) {
                        url = "https://reddit.com" + data.path("permalink").asText();
                    } else {
                        url = data.path("url").asText();
                    }

                    String selftext = data.path("selftext").asText();
                    String description = selftext.length() > 250
                        ? selftext.substring(0, 250) + "..."
                        : selftext;

                    items.add(NewsItem.builder()
                        .title(title)
                        .url(url)
                        .description(description.isBlank() ? null : description)
                        .score(data.path("score").asInt())
                        .source("Reddit r/" + subreddit)
                        .build());
                }

                Thread.sleep(600);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to fetch Reddit r/{}: {}", subreddit, e.getMessage());
            }
        }

        return items.stream()
            .sorted(Comparator.comparingInt(NewsItem::getScore).reversed())
            .toList();
    }
}
