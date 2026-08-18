package com.omar.chatclient_vs_chatmodel.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Demonstrates {@link ChatClient}: Spring AI's high-level, fluent, builder-style API,
 * similar in shape to {@code RestClient}. This is the entry point recommended for
 * almost every Spring AI project - it reads cleanly and hides the request/response
 * plumbing that {@link org.springframework.ai.chat.model.ChatModel} exposes directly.
 */
@Service
public class ChatClientService {

    private final ChatClient chatClient;

    public ChatClientService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Simplest possible call: prompt in, plain-text answer out.
     */
    public String ask(String userMessage) {
        return chatClient
                .prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Same fluent chain, but with a per-request system prompt overriding the
     * default one configured on the bean - useful when a single ChatClient
     * needs to play different "roles" depending on the endpoint calling it.
     */
    public String askWithSystemPrompt(String systemPrompt, String userMessage) {
        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

}
