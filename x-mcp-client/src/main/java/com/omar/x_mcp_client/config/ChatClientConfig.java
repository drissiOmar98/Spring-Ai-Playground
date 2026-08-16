package com.omar.x_mcp_client.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application's single {@link ChatClient} with every tool discovered
 * from the configured MCP connections (currently {@code x-docs} and {@code x-api},
 * see {@code application.yaml}).
 *
 * <p>Spring AI's MCP client auto-configuration builds the {@link ToolCallbackProvider}
 * for us by aggregating the tool lists returned by each connected MCP server; we just
 * hand it to the {@link ChatClient.Builder}. This is what lets Claude "see" both the
 * docs-search tools and the live X API tools in the same conversation and pick
 * whichever one fits the question.
 */
@Configuration
class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider tools) {
        return builder.defaultTools(tools).build();
    }
}
