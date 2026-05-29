package com.brewerbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.github.token=test-token",
    "app.github.username=test-user",
    "app.telegram.bot-token=123:test",
    "app.telegram.channel-id=@test",
    "app.anthropic.api-key=sk-ant-test"
})
class BrewerBotApplicationTests {

    @Test
    void contextLoads() {
    }
}
