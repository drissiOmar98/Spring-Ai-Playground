package com.omar.x_mcp_client.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo 2: the live X API MCP server ({@code https://api.x.com/mcp}), reached over
 * streamable HTTP with a bearer token attached by {@link com.omar.x_mcp_client.config.XApiAuthConfig}.
 *
 * <p>{@code GET /search?topic=...} asks Claude to search recent posts on a topic and
 * summarize what people are saying. Requires the {@code x-api} connection to be
 * enabled in {@code application.yaml} and {@code X_BEARER_TOKEN} to be set &mdash; see
 * the README, including the auth caveat in {@link com.omar.x_mcp_client.config.XApiAuthConfig}.
 */
@RestController
class XApiController {

    private static final Logger log = LoggerFactory.getLogger(XApiController.class);

    private final ChatClient chatClient;

    XApiController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping(value = "/search", produces = "text/plain")
    String search(@RequestParam(defaultValue = "Spring Boot 4.1") String topic) {
        var question = """
                Search X for recent posts about "%s" and give me a quick read on
                what developers are talking about. Pull out a few themes and
                mention anything notable. Keep it tight.
                """.formatted(topic);
        log.info("Asking the X API MCP server (live search) for topic: {}", topic);
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
