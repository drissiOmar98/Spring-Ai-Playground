package com.omar.tool_calling_streaming.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the shared {@link ChatClient} used by the travel chat endpoint.
 *
 * <p>Only the memory advisor is registered as a default here.
 * {@code ToolCallingAdvisor} is added implicitly by Spring AI whenever tools
 * are attached to a request (see {@code ChatController}), and the
 * per-request {@code ToolCallObservingAdvisor} is added per-call since it
 * carries per-turn state and cannot be shared across requests.
 *
 * <p>Ordering matters: {@link MessageChatMemoryAdvisor} runs at
 * {@code HIGHEST_PRECEDENCE + 200}, outside the tool-calling loop
 * (order {@code + 300}). That means memory only ever sees the original user
 * message and the final assistant answer for a turn — never the
 * intermediate tool-call iterations.
 */
@Configuration
public class ChatClientConfig {

    /**
     * Builds the {@link ChatClient} used for every travel chat turn.
     *
     * @param builder    the auto-configured {@link ChatClient.Builder}
     * @param chatMemory the conversation memory store, used to keep one
     *                   history per {@code conversationId}
     * @return a {@link ChatClient} pre-configured with conversation memory
     */
    @Bean
    public ChatClient travelChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}