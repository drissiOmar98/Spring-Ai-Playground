package com.omar.toolsearch.controller;

import java.util.UUID;

import com.omar.toolsearch.advisors.AvailableToolsLoggingAdvisor;
import com.omar.toolsearch.advisors.TokenCounterAdvisor;
import com.omar.toolsearch.tools.InitechTools;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Demonstrates three tool-loading strategies against the same question, so
 * you can compare their token cost side by side.
 *
 * <ul>
 *   <li>{@link #before} — no tools registered at all (baseline).</li>
 *   <li>{@link #after} — all 47 tools loaded into every request (the naive
 *       approach; expensive as the tool count grows).</li>
 *   <li>{@link #toolSearch} — tools registered but not sent upfront; the model
 *       discovers only what it needs via the Tool Search advisor.</li>
 * </ul>
 */
@RestController
public class ChatController {

    private static final String DEFAULT_QUESTION = "What is tomorrow's date?";

    private final ChatClient chatClient;
    private final InitechTools tools;

    public ChatController(ChatClient chatClient, InitechTools tools) {
        this.chatClient = chatClient;
        this.tools = tools;
    }

    /**
     * Baseline: no tools registered. Expect the lowest possible token count,
     * since the model only ever sees the raw prompt.
     */
    @GetMapping("/before")
    public @Nullable String before(@RequestParam(defaultValue = DEFAULT_QUESTION) String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(new TokenCounterAdvisor(), new AvailableToolsLoggingAdvisor())
                .call()
                .content();
    }

    /**
     * Naive approach: every tool's full definition (name, description, JSON
     * schema) is serialized into every request, whether or not it's needed.
     * With 47 tools this is where the ~5,100+ prompt-token cost comes from.
     */
    @GetMapping("/after")
    public @Nullable String after(@RequestParam(defaultValue = DEFAULT_QUESTION) String message) {
        return chatClient.prompt()
                .user(message)
                .tools(tools)
                .advisors(new TokenCounterAdvisor(), new AvailableToolsLoggingAdvisor())
                .call()
                .content();
    }

    /**
     * Tool Search approach: tools are registered but NOT sent to the model
     * upfront. The model instead sees a single "search" tool, queries it in
     * natural language, and only the matching tool definitions (capped by
     * {@code max-results} in application.yaml) get added to the context.
     *
     * <p>A per-request {@code ChatMemory.CONVERSATION_ID} is required — the
     * advisor uses it to track which tools have already been discovered in
     * this session. Without it, the advisor throws at runtime.
     */
    @GetMapping("/tool-search")
    public @Nullable String toolSearch(@RequestParam(defaultValue = DEFAULT_QUESTION) String message) {
        return chatClient.prompt()
                .user(message)
                .tools(tools)
                .advisors(new TokenCounterAdvisor(), new AvailableToolsLoggingAdvisor())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, UUID.randomUUID().toString()))
                .call()
                .content();
    }
}