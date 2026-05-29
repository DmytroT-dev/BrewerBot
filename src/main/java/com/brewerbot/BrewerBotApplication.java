package com.brewerbot;

import com.brewerbot.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
public class BrewerBotApplication {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(BrewerBotApplication.class, args);
        var props = ctx.getBean(AppProperties.class);
        log.info("BrewerBot started. Monitoring GitHub user: {}", props.getGithub().getUsername());
    }
}
