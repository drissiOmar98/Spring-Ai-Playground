package com.omar.chatclient_vs_chatmodel.controller;


import com.omar.chatclient_vs_chatmodel.dto.ChatMetadataResponse;
import com.omar.chatclient_vs_chatmodel.service.ChatModelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints exercising the low-level {@link org.springframework.ai.chat.model.ChatModel}
 * interface - the one {@link org.springframework.ai.chat.client.ChatClient} is built on
 * top of. Use this when you need raw response metadata or provider features that the
 * fluent API doesn't surface, such as multiple generations per call.
 */
@RestController
public class ChatModelController {

    private static final String DEFAULT_PROMPT = """
            In two short paragraphs, explain the difference between Spring AI's
            ChatClient and ChatModel, aimed at a Java developer.
            """;

    private static final String METADATA_PROMPT =
            "In one sentence, explain what a Large Language Model is.";

    private static final String MULTI_GENERATION_PROMPT =
            "Suggest one good name for a Spring AI demo GitHub repository.";

    private final ChatModelService chatModelService;

    public ChatModelController(ChatModelService chatModelService) {
        this.chatModelService = chatModelService;
    }

    /**
     * GET /api/chat-model/ask?message=...
     * Plain-text answer via the raw ChatModel call - the equivalent of
     * ChatClientController#ask, but going through ChatResponse directly.
     */
    @GetMapping("/api/chat-model/ask")
    public String ask(@RequestParam(required = false) String message) {
        String userMessage = (message != null && !message.isBlank()) ? message : DEFAULT_PROMPT;
        return chatModelService.ask(userMessage);
    }

    /**
     * GET /api/chat-model/metadata
     * Same kind of call, but returning the model name, finish reason, and
     * prompt/completion/total token usage that ChatClient's .content() hides.
     */
    @GetMapping("/api/chat-model/metadata")
    public ChatMetadataResponse metadata() {
        return chatModelService.askWithMetadata(METADATA_PROMPT);
    }



}
