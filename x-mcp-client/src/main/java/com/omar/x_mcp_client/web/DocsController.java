package com.omar.x_mcp_client.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo 1: the X docs MCP server ({@code https://docs.x.com/mcp}), read-only and
 * unauthenticated.
 *
 * <p>{@code GET /docs} asks a fixed question about the X API and lets Claude call
 * whichever documentation-search tool the {@code x-docs} MCP connection exposes.
 * Works with no credentials configured.
 */
@RestController
class DocsController {

    private static final Logger log = LoggerFactory.getLogger(DocsController.class);

    private final ChatClient chatClient;

    DocsController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping(value = "/docs", produces = "text/plain")
    String docs() {
        var question = """
                Using the X API docs, how do I look up a user and find the number
                of followers they have? Give me the endpoint and the key query
                parameters. Keep it short.
                """;
        log.info("Asking the X docs MCP server...");
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
