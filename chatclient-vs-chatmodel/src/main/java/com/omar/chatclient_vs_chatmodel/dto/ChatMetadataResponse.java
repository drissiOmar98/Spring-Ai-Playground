package com.omar.chatclient_vs_chatmodel.dto;

/**
 * Response shape exposing the raw metadata that {@link org.springframework.ai.chat.model.ChatModel}
 * gives you access to, but that {@link org.springframework.ai.chat.client.ChatClient}'s
 * simple {@code .content()} shortcut hides from you.
 */
public record ChatMetadataResponse(

        String answer,

        String modelName,

        String finishReason,

        Integer promptTokens,

        Integer completionTokens,

        Integer totalTokens

) {
}
