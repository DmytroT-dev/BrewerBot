package com.brewerbot.service;

import com.brewerbot.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class TelegramPublisherService {

    private static final int TELEGRAM_MAX_LENGTH = 4096;

    private final WebClient telegramClient;
    private final AppProperties props;

    public TelegramPublisherService(@Qualifier("telegramWebClient") WebClient telegramClient,
                                    AppProperties props) {
        this.telegramClient = telegramClient;
        this.props = props;
    }

    public boolean sendMessage(String text) {
        String safeText = text.length() > TELEGRAM_MAX_LENGTH
            ? text.substring(0, TELEGRAM_MAX_LENGTH - 3) + "..."
            : text;

        try {
            JsonNode response = telegramClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of(
                    "chat_id", props.getTelegram().getChannelId(),
                    "text", safeText,
                    "parse_mode", "HTML",
                    "disable_web_page_preview", false
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            boolean ok = response != null && response.path("ok").asBoolean();
            if (!ok) {
                log.error("Telegram API returned not-ok: {}", response);
            }
            return ok;

        } catch (WebClientResponseException e) {
            log.error("Telegram API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Try sending as plain text if HTML parse failed (Telegram returns 400)
            if (e.getStatusCode().value() == 400) {
                return sendPlainText(safeText);
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to send Telegram message: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendPlainText(String text) {
        try {
            JsonNode response = telegramClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of(
                    "chat_id", props.getTelegram().getChannelId(),
                    "text", text
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            return response != null && response.path("ok").asBoolean();
        } catch (Exception e) {
            log.error("Plain text fallback also failed: {}", e.getMessage());
            return false;
        }
    }
}
