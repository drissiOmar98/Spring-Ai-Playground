package com.omar.spring_ai_custom_advisors.advisor;

import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.lang.Nullable;

/**
 * 🤖 Lightweight request/response logger — the "hello world" of Spring AI advisors.
 *
 * <p>Logs the raw {@link ChatClientRequest} and {@link ChatResponse} at debug level,
 * using pluggable {@code toString} functions so callers can trim what gets logged
 * (e.g. to redact large prompts) without subclassing.
 */
public class EmojiLoggingAdvisor implements CallAdvisor {

	public static final Function<ChatClientRequest, String> DEFAULT_REQUEST_TO_STRING = ChatClientRequest::toString;
	public static final Function<ChatResponse, String> DEFAULT_RESPONSE_TO_STRING = EmojiLoggingAdvisor::toJson;

	private static final Logger logger = LoggerFactory.getLogger(EmojiLoggingAdvisor.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final Function<ChatClientRequest, String> requestToString;
	private final Function<ChatResponse, String> responseToString;
	private final int order;

	/**
	 * Creates a logger with default {@code toString} formatting and order {@code 0}.
	 */
	public EmojiLoggingAdvisor() {
		this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, 0);
	}

	/**
	 * Creates a logger with default {@code toString} formatting at an explicit order.
	 *
	 * @param order the advisor chain order
	 */
	public EmojiLoggingAdvisor(int order) {
		this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, order);
	}

	/**
	 * Creates a logger with custom formatting functions.
	 *
	 * @param requestToString  formats the request for logging; falls back to
	 *                         {@link #DEFAULT_REQUEST_TO_STRING} when {@code null}
	 * @param responseToString formats the response for logging; falls back to
	 *                         {@link #DEFAULT_RESPONSE_TO_STRING} when {@code null}
	 * @param order            the advisor chain order
	 */
	public EmojiLoggingAdvisor(@Nullable Function<ChatClientRequest, String> requestToString,
			@Nullable Function<ChatResponse, String> responseToString, int order) {
		this.requestToString = requestToString != null ? requestToString : DEFAULT_REQUEST_TO_STRING;
		this.responseToString = responseToString != null ? responseToString : DEFAULT_RESPONSE_TO_STRING;
		this.order = order;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		logRequest(chatClientRequest);
		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
		logResponse(chatClientResponse);
		return chatClientResponse;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	private void logRequest(ChatClientRequest request) {
		logger.debug("\uD83E\uDD16 Request: {}", this.requestToString.apply(request));
	}

	private void logResponse(ChatClientResponse chatClientResponse) {
		logger.debug("\uD83E\uDD16 Response: {}", this.responseToString.apply(chatClientResponse.chatResponse()));
	}

	/**
	 * Serializes a {@link ChatResponse} to JSON for logging. Replaces the deprecated
	 * {@code org.springframework.ai.util.json.JsonParser}; falls back to
	 * {@link Object#toString()} if serialization fails (e.g. a non-serializable field)
	 * rather than throwing from inside a logging call.
	 */
	private static String toJson(@Nullable ChatResponse response) {
		if (response == null) {
			return "null";
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(response);
		}
		catch (Exception ex) {
			return response.toString();
		}
	}

}
