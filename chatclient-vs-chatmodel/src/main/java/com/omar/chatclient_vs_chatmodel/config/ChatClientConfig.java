package com.omar.chatclient_vs_chatmodel.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the {@link ChatClient} bean used across the "high-level API" side of this demo.
 * <p>
 * Spring AI auto-configures a {@link ChatClient.Builder} for whichever provider is on
 * the classpath (OpenAI here). We only need to customize it once, at startup, with a
 * default system prompt - every controller then injects the finished {@code ChatClient}
 * directly instead of rebuilding it per request.
 */
@Configuration
public class ChatClientConfig {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful assistant with deep expertise in Java and the Spring
            ecosystem, especially Spring AI. Keep answers accurate, concise, and
            practical for a developer audience.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }

}
