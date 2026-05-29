package com.brewerbot.service;

import com.brewerbot.config.AppProperties;
import com.brewerbot.model.BotState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;

@Service
@Slf4j
public class StateManager {

    private static final int MAX_POSTED_URLS = 1000;

    private final ObjectMapper objectMapper;
    private final Path stateFile;
    private BotState state;

    public StateManager(AppProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.stateFile = Path.of(props.getState().getFilePath());
        this.state = loadState();
    }

    private BotState loadState() {
        if (Files.exists(stateFile)) {
            try {
                return objectMapper.readValue(stateFile.toFile(), BotState.class);
            } catch (IOException e) {
                log.warn("Could not load state file, starting fresh: {}", e.getMessage());
            }
        }
        return new BotState();
    }

    public synchronized void saveState() {
        try {
            objectMapper.writeValue(stateFile.toFile(), state);
        } catch (IOException e) {
            log.error("Failed to save state: {}", e.getMessage());
        }
    }

    public synchronized boolean isNewsAlreadyPosted(String url) {
        return state.getPostedNewsUrls().contains(url);
    }

    public synchronized void markNewsAsPosted(String url) {
        if (!state.getPostedNewsUrls().contains(url)) {
            state.getPostedNewsUrls().add(url);
            if (state.getPostedNewsUrls().size() > MAX_POSTED_URLS) {
                int excess = state.getPostedNewsUrls().size() - MAX_POSTED_URLS;
                state.setPostedNewsUrls(new ArrayList<>(state.getPostedNewsUrls().subList(excess, state.getPostedNewsUrls().size())));
            }
            saveState();
        }
    }

    public synchronized String getLastCommitSha(String repoFullName) {
        return state.getLastCommitShas().get(repoFullName);
    }

    public synchronized void updateLastCommitSha(String repoFullName, String sha) {
        state.getLastCommitShas().put(repoFullName, sha);
        saveState();
    }

    public synchronized void updateLastJavaNewsCheck() {
        state.setLastJavaNewsCheck(Instant.now());
        saveState();
    }

    public synchronized void updateLastAiNewsCheck() {
        state.setLastAiNewsCheck(Instant.now());
        saveState();
    }
}
