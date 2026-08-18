package com.omar.chatclient_vs_chatmodel.service;


import com.omar.chatclient_vs_chatmodel.dto.ChatMetadataResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;


/**
 * Demonstrates {@link ChatModel}: the low-level interface that {@link
 * org.springframework.ai.chat.client.ChatClient} is built on top of. Both APIs
 * ultimately call the same provider - ChatModel just hands you the raw
 * {@link ChatResponse} instead of collapsing it down to a plain string, which is
 * where things like token usage, finish reason, and multiple generations live.
 */
@Service
public class ChatModelService {

    private final ChatModel chatModel;

    public ChatModelService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Simplest possible call - equivalent in outcome to ChatClientService#ask,
     * but going through the raw ChatResponse to get there.
     */
    public String ask(String userMessage) {
        ChatResponse response = chatModel.call(new Prompt(userMessage));
        return response.getResult().getOutput().getText();
    }

    /**
     * Same prompt as {@link #ask}, but returns everything ChatClient's
     * {@code .content()} shortcut throws away: model name, finish reason,
     * and prompt/completion/total token usage.
     */
    public ChatMetadataResponse askWithMetadata(String userMessage) {
        ChatResponse response = chatModel.call(new Prompt(userMessage));

        Generation generation = response.getResult();
        AssistantMessage output = generation.getOutput();

        String finishReason = generation.getMetadata() != null
                ? generation.getMetadata().getFinishReason()
                : null;

        String modelName = response.getMetadata() != null
                ? response.getMetadata().getModel()
                : null;

        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;

        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            var usage = response.getMetadata().getUsage();
            promptTokens = usage.getPromptTokens();
            completionTokens = usage.getCompletionTokens();
            totalTokens = usage.getTotalTokens();
        }

        return new ChatMetadataResponse(
                output.getText(),
                modelName,
                finishReason,
                promptTokens,
                completionTokens,
                totalTokens
        );
    }


}
