package com.omar.chatclient_vs_chatmodel.dto;

import java.util.List;

/**
 * Response shape for the multi-generation demo: a single call to
 * {@link org.springframework.ai.chat.model.ChatModel} can ask the provider for
 * several independent completions ({@code n > 1}) of the same prompt. ChatClient's
 * fluent API only ever surfaces the first one, so this is a ChatModel-only feature.
 */
public record MultiGenerationResponse(

        String prompt,

        int requestedGenerations,

        List<String> generations

) {
}
