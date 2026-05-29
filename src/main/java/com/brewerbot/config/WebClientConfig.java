package com.brewerbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("anthropicWebClient")
    public WebClient anthropicWebClient(AppProperties props) {
        return WebClient.builder()
            .baseUrl("https://api.anthropic.com")
            .defaultHeader("x-api-key", props.getAnthropic().getApiKey())
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Bean("telegramWebClient")
    public WebClient telegramWebClient(AppProperties props) {
        return WebClient.builder()
            .baseUrl("https://api.telegram.org/bot" + props.getTelegram().getBotToken())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }

    @Bean("hnWebClient")
    public WebClient hackerNewsWebClient() {
        return WebClient.builder()
            .baseUrl("https://hacker-news.firebaseio.com/v0")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
            .build();
    }

    @Bean("redditWebClient")
    public WebClient redditWebClient() {
        return WebClient.builder()
            .baseUrl("https://www.reddit.com")
            .defaultHeader(HttpHeaders.USER_AGENT, "BrewerBot/1.0 (by /u/your_username)")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
            .build();
    }
}
