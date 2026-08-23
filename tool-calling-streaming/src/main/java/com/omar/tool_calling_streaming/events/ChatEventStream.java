package com.omar.tool_calling_streaming.events;


import org.springframework.http.codec.ServerSentEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

/**
 * Per-request bundle of the SSE plumbing for one chat turn: the side channel
 * ({@link #toolEvents}) tool events are pushed onto, the advisor that feeds
 * it, and the assembly of the final merged stream.
 *
 * <p>Event vocabulary: {@code token | tool | done | error}. Token and tool
 * payloads are JSON-encoded — raw SSE data would lose leading spaces on
 * tokens, because the SSE spec strips one space after {@code "data:"}.
 */
public class ChatEventStream {

    private final JsonMapper json;
    private final Sinks.Many<ServerSentEvent<String>> toolEvents = Sinks.many().unicast().onBackpressureBuffer();
    private final ToolCallObservingAdvisor observer;

    public ChatEventStream(JsonMapper json) {
        this.json = json;
        this.observer = new ToolCallObservingAdvisor(
                event -> this.toolEvents.tryEmitNext(sse("tool", json.writeValueAsString(event))));
    }

    /** The advisor to register on the request — pushes tool events onto the side channel. */
    public ToolCallObservingAdvisor observer() {
        return this.observer;
    }

    /** Wraps one answer token as an SSE event. */
    public ServerSentEvent<String> token(String token) {
        return sse("token", this.json.writeValueAsString(token));
    }

    /**
     * Merges the answer tokens with the tool-event side channel and appends
     * the terminal events: upstream errors become an {@code "error"} event
     * instead of killing the stream, and {@code "done"} always closes the
     * turn. Completing the sink when the token flux ends is what lets the
     * merged stream terminate — without it the browser would hang.
     */
    public Flux<ServerSentEvent<String>> mergeWith(Flux<ServerSentEvent<String>> tokens) {
        return Flux.merge(tokens.doFinally(signal -> this.toolEvents.tryEmitComplete()), this.toolEvents.asFlux())
                .onErrorResume(e -> Flux.just(sse("error", errorMessage(e))))
                .concatWith(Flux.just(sse("done", "")));
    }

    private static ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.builder(data).event(event).build();
    }

    private static String errorMessage(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}