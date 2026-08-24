package com.omar.tool_calling_streaming.events;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * Sits INSIDE {@code ToolCallingAdvisor}'s recursive loop (order is greater
 * than {@link ToolCallingAdvisor#DEFAULT_ORDER}, i.e. closer to the model),
 * so it sees every intermediate chunk that the tool loop normally hides from
 * the outbound stream — including the assistant chunks that carry tool
 * calls.
 *
 * <p>Two observation points:
 * <ul>
 *   <li>{@code STARTED} — a streamed chunk's generation carries a tool call.
 *       Arguments arrive as partial JSON fragments across chunks, so calls
 *       are de-duplicated on the call id and only the name is reported.</li>
 *   <li>{@code COMPLETED} — on the next loop iteration, the incoming
 *       request's message history contains the {@link ToolResponseMessage}s
 *       from the previous round. Matching response ids against previously
 *       started calls signals those tools finished.</li>
 * </ul>
 *
 * <p>Create one instance per request: it keeps per-conversation-turn state
 * ({@link #started} / {@link #completed}) that must not be shared across
 * concurrent requests.
 */
public class ToolCallObservingAdvisor implements CallAdvisor, StreamAdvisor {

    /** Inside the tool loop: {@code ToolCallingAdvisor.DEFAULT_ORDER} is {@code MIN_VALUE + 300}. */
    public static final int ORDER = ToolCallingAdvisor.DEFAULT_ORDER + 100;

    private final Consumer<ToolCallEvent> listener;
    private final Set<String> started = ConcurrentHashMap.newKeySet();
    private final Set<String> completed = ConcurrentHashMap.newKeySet();

    /**
     * @param listener callback invoked with each observed {@link ToolCallEvent},
     *                 in the order they occur
     */
    public ToolCallObservingAdvisor(Consumer<ToolCallEvent> listener) {
        this.listener = listener;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        emitCompletions(request);
        return chain.nextStream(request).doOnNext(this::emitStarts);
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        emitCompletions(request);
        ChatClientResponse response = chain.nextCall(request);
        emitStarts(response);
        return response;
    }

    /** Emits a {@code STARTED} event for any tool call not seen before. */
    private void emitStarts(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return;
        }
        chatResponse.getResults().stream()
                .flatMap(generation -> generation.getOutput().getToolCalls().stream())
                .filter(toolCall -> StringUtils.hasText(toolCall.id()) && StringUtils.hasText(toolCall.name()))
                .filter(toolCall -> this.started.add(toolCall.id()))
                .forEach(toolCall -> this.listener
                        .accept(new ToolCallEvent(toolCall.id(), toolCall.name(), ToolCallEvent.Phase.STARTED)));
    }

    /** Emits a {@code COMPLETED} event for any previously started call whose response just arrived. */
    private void emitCompletions(ChatClientRequest request) {
        request.prompt().getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .flatMap(message -> message.getResponses().stream())
                .filter(response -> this.started.contains(response.id()) && this.completed.add(response.id()))
                .forEach(response -> this.listener
                        .accept(new ToolCallEvent(response.id(), response.name(), ToolCallEvent.Phase.COMPLETED)));
    }

    @Override
    public String getName() {
        return "toolCallObservingAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}