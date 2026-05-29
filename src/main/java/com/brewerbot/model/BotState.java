package com.brewerbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BotState {
    private Map<String, String> lastCommitShas = new HashMap<>();
    private List<String> postedNewsUrls = new ArrayList<>();
    private Instant lastJavaNewsCheck = Instant.EPOCH;
    private Instant lastAiNewsCheck = Instant.EPOCH;
}
