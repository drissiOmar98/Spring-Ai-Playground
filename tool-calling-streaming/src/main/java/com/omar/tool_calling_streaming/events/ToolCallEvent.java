package com.omar.tool_calling_streaming.events;

/**
 * A tool lifecycle event observed inside the tool-calling loop.
 * Serialized to the browser as the SSE {@code "tool"} event.
 *
 * @param id    the tool call id assigned by the model, used to correlate a
 *              {@link Phase#STARTED} event with its later
 *              {@link Phase#COMPLETED} event
 * @param name  the tool's name, e.g. {@code "getWeatherForecast"}
 * @param phase whether the call has just started or just completed
 */
public record ToolCallEvent(String id, String name, Phase phase) {

    /** Lifecycle stages of a single tool call. */
    public enum Phase {
        /** The model has requested this tool call; execution is starting. */
        STARTED,
        /** The tool has returned a result back to the model. */
        COMPLETED
    }
}