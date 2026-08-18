package com.omar.chatclient_vs_chatmodel.controller;


import com.omar.chatclient_vs_chatmodel.service.ChatClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints exercising the high-level {@link org.springframework.ai.chat.client.ChatClient}
 * API - the fluent, RestClient-style entry point Spring AI recommends starting with.
 */
@RestController
public class ChatClientController {

    private static final String DEFAULT_PROMPT = """
            In two short paragraphs, explain what Spring AI is and why a Java
            developer already familiar with Spring Boot would want to use it.
            """;

    private final ChatClientService chatClientService;

    public ChatClientController(ChatClientService chatClientService) {
        this.chatClientService = chatClientService;
    }

    /**
     * GET /api/chat-client/ask?message=...
     * Plain-text answer via the fluent ChatClient chain. Uses a canned Spring AI
     * prompt by default so the endpoint works out of the box with no query params.
     */
    @GetMapping("/api/chat-client/ask")
    public String ask(@RequestParam(required = false) String message) {
        String userMessage = (message != null && !message.isBlank()) ? message : DEFAULT_PROMPT;
        return chatClientService.ask(userMessage);
    }

    /**
     * GET /api/chat-client/ask-as
     * Same fluent API, but overriding the default system prompt per request -
     * shows ChatClient's builder chain composing system + user messages.
     */
    @GetMapping("/api/chat-client/ask-as")
    public String askAs() {
        String systemPrompt = "You are a terse senior Java architect. Answer in bullet points only.";
        String userMessage = "What are the main building blocks of Spring AI's ChatClient API?";
        return chatClientService.askWithSystemPrompt(systemPrompt, userMessage);
    }

}
