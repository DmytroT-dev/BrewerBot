package com.brewerbot.service;

import com.brewerbot.config.AppProperties;
import com.brewerbot.model.CommitDiff;
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
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentGeneratorService {

    private static final Duration CLAUDE_TIMEOUT = Duration.ofSeconds(90);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d+)\\b");

    private final WebClient anthropicClient;
    private final AppProperties props;

    public ContentGeneratorService(@Qualifier("anthropicWebClient") WebClient anthropicClient,
                                   AppProperties props) {
        this.anthropicClient = anthropicClient;
        this.props = props;
    }

    public Optional<String> generateCodePost(CommitDiff diff) {
        String filesContent = diff.getFiles().stream()
            .map(f -> "Файл: " + f.getFilename() + "\n```diff\n" + f.getPatch() + "\n```")
            .collect(Collectors.joining("\n\n"));

        String prompt = """
            Ты — Java/Spring-эксперт и автор популярного Telegram-канала в стиле @java_fillthegaps.

            Проанализируй этот Git-коммит и создай образовательный пост для Telegram-канала.

            Репозиторий: %s
            Сообщение коммита: %s
            Автор: %s

            Изменения:
            %s

            ЗАДАЧА: найди в diff самый интересный паттерн, фичу или best practice Java/Spring.
            Если ничего интересного нет — ответь только словом SKIP.

            ФОРМАТ ПОСТА (HTML для Telegram):
            - Начни с эмодзи + <b>краткий заголовок</b>
            - 2-3 предложения: объясни концепцию, зачем она нужна
            - Пример кода в <pre><code class="language-java">...</code></pre>
              (покажи суть паттерна, не весь diff — чистый, понятный пример)
            - Один практический совет или "почему это важно"
            - 3-5 хэштегов через пробел: #java #spring и т.д.

            Пиши по-русски. Длина текста (без кода): 200-500 символов.
            Используй ТОЛЬКО разрешённые HTML-теги Telegram: <b>, <i>, <pre>, <code>, <a href="">.
            """.formatted(diff.getRepoName(), diff.getCommitMessage(), diff.getAuthorName(), filesContent);

        Optional<String> result = callClaude(prompt, 1500);
        return result.filter(text -> !text.trim().equalsIgnoreCase("SKIP"));
    }

    public Optional<String> generateNewsPost(NewsItem item, String category) {
        String categoryLabel = "java-spring".equals(category)
            ? "Java/Spring-разработка"
            : "Искусственный интеллект и AI";

        String prompt = """
            Ты — куратор Telegram-канала о технологиях (стиль: коротко, понятно, по делу).

            Создай пост о следующей новости для аудитории %s.

            Заголовок: %s
            Источник: %s
            Ссылка: %s
            Описание: %s

            ФОРМАТ (HTML для Telegram):
            - Эмодзи + <b>заголовок</b> (своими словами, цепляющий)
            - 2-3 предложения: суть новости и почему это важно для разработчиков
            - <a href="%s">Читать →</a>
            - 3-5 хэштегов

            Пиши по-русски. Только разрешённые HTML-теги Telegram: <b>, <i>, <a href="">.
            Длина (без ссылки и хэштегов): 100-300 символов.
            """.formatted(categoryLabel, item.getTitle(), item.getSource(),
                          item.getUrl(), item.getDescription() != null ? item.getDescription() : "—",
                          item.getUrl());

        return callClaude(prompt, 800);
    }

    public List<NewsItem> rankAndSelectNews(List<NewsItem> candidates, String category, int topN) {
        if (candidates.size() <= topN) {
            return new ArrayList<>(candidates);
        }

        int limit = Math.min(candidates.size(), 20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            NewsItem item = candidates.get(i);
            sb.append(i + 1).append(". ")
              .append(item.getTitle())
              .append(" (score: ").append(item.getScore())
              .append(", ").append(item.getSource()).append(")\n");
        }

        String categoryLabel = "java-spring".equals(category)
            ? "Java/Spring/JVM"
            : "AI/ML/LLM";

        String prompt = """
            Ты — куратор Telegram-канала о %s.
            Выбери TOP %d самых интересных и важных новостей из списка ниже.

            Критерии: важные релизы, прорывы, практичные инсайты, вирусные темы.
            Избегай: кликбейт, устаревшие темы, слишком технические нишевые вещи.

            Список:
            %s

            Ответь ТОЛЬКО номерами выбранных пунктов через запятую, например: 2, 5, 11
            """.formatted(categoryLabel, topN, sb.toString());

        try {
            Optional<String> response = callClaude(prompt, 100);
            if (response.isEmpty()) {
                return topByScore(candidates, topN);
            }

            List<Integer> indices = new ArrayList<>();
            Matcher matcher = NUMBER_PATTERN.matcher(response.get());
            while (matcher.find()) {
                int n = Integer.parseInt(matcher.group(1));
                if (n >= 1 && n <= limit && !indices.contains(n)) {
                    indices.add(n);
                }
            }

            if (indices.isEmpty()) {
                return topByScore(candidates, topN);
            }

            return indices.stream()
                .limit(topN)
                .map(i -> candidates.get(i - 1))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("News ranking failed, falling back to score-based: {}", e.getMessage());
            return topByScore(candidates, topN);
        }
    }

    private List<NewsItem> topByScore(List<NewsItem> items, int n) {
        return items.stream()
            .sorted(Comparator.comparingInt(NewsItem::getScore).reversed())
            .limit(n)
            .collect(Collectors.toList());
    }

    private Optional<String> callClaude(String userMessage, int maxTokens) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", props.getAnthropic().getModel(),
                "max_tokens", maxTokens,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            JsonNode response = anthropicClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(CLAUDE_TIMEOUT)
                .block();

            if (response == null) {
                return Optional.empty();
            }

            JsonNode content = response.path("content");
            if (content.isArray() && !content.isEmpty()) {
                String text = content.get(0).path("text").asText();
                if (!text.isBlank()) {
                    return Optional.of(text.trim());
                }
            }

            log.warn("Unexpected Claude response: {}", response);
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
