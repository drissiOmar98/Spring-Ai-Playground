package com.omar.tool_calling_streaming.web;


import com.omar.tool_calling_streaming.events.ChatEventStream;
import com.omar.tool_calling_streaming.tools.TravelTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

/**
 * One SSE stream per chat turn: answer tokens from the {@link ChatClient},
 * merged with tool lifecycle events observed inside the tool-calling loop.
 *
 * <p>The SSE plumbing lives in {@link ChatEventStream}; the observing
 * advisor in {@link com.omar.tool_calling_streaming.events.ToolCallObservingAdvisor}.
 * The {@link ChatClient} itself — including its default advisors — is
 * configured separately in {@code ChatClientConfig}.
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final TravelTools travelTools;
    private final JsonMapper jsonMapper;

    public ChatController(ChatClient travelChatClient, TravelTools travelTools, JsonMapper jsonMapper) {
        this.chatClient = travelChatClient;
        this.travelTools = travelTools;
        this.jsonMapper = jsonMapper;
    }

    /** Request body for a single chat turn. */
    public record ChatRequest(String message, String conversationId) {
    }

    /**
     * Streams one chat turn as SSE: {@code token} events for each answer
     * chunk, {@code tool} events for each tool call's start/completion, and
     * a terminal {@code error} or {@code done} event.
     *
     * @param request the user's message and the conversation to append it to
     * @return the merged SSE stream for this turn
     */
    @PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody ChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId()) ? request.conversationId() : "default";
        ChatEventStream events = new ChatEventStream(this.jsonMapper);

        Flux<ServerSentEvent<String>> answerTokens = this.chatClient.prompt()
                .user(request.message())
                .tools(this.travelTools)
                .advisors(a -> a.advisors(events.observer())
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .map(events::token);

        return events.mergeWith(answerTokens);
    }
}