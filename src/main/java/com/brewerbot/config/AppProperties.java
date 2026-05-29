package com.brewerbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private GitHub github = new GitHub();
    private Telegram telegram = new Telegram();
    private Anthropic anthropic = new Anthropic();
    private Scheduling scheduling = new Scheduling();
    private State state = new State();

    @Data
    public static class GitHub {
        private String token;
        private String username;
        private List<String> repos = new ArrayList<>();
    }

    @Data
    public static class Telegram {
        private String botToken;
        private String channelId;
    }

    @Data
    public static class Anthropic {
        private String apiKey;
        private String model = "claude-sonnet-4-6";
    }

    @Data
    public static class Scheduling {
        private String githubCheckCron = "0 */30 * * * *";
        private String javaNewsCron = "0 0 */3 * * *";
        private String aiNewsCron = "0 30 */4 * * *";
    }

    @Data
    public static class State {
        private String filePath = "./bot-state.json";
    }
}
