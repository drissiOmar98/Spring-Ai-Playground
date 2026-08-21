package com.omar.toolsearch.advisors;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * Prints, for every LLM call in the tool-calling loop, the exact tools visible to the model
 * and the tools it chooses to call. Must be ordered <em>after</em> the Tool Search advisor
 * ({@code HIGHEST_PRECEDENCE + 300}) so it observes the trimmed tool set each iteration —
 * never the full catalog. The default constructor already picks a value that satisfies this;
 * see {@link #DEFAULT_ORDER}.
 */
public class AvailableToolsLoggingAdvisor implements BaseAdvisor {

	/**
	 * Default chain order for this advisor. Any value greater than the tool-search advisor's
	 * order makes this logger run <em>after</em> tool search, so it observes the trimmed tool
	 * set for each iteration rather than the full catalog. The tool-search advisor runs at
	 * {@code ToolCallingAdvisor.DEFAULT_ORDER}
	 * ({@code Ordered.HIGHEST_PRECEDENCE + 300 = -2147483348}), so any comfortably positive
	 * value satisfies the constraint; {@code 1000} is used here.
	 */
	public static final int DEFAULT_ORDER = 1000;

	private static final String CYAN = "\u001B[36m";

	private static final String GREEN = "\u001B[32m";

	private static final String DIM = "\u001B[2m";

	private static final String RESET = "\u001B[0m";

	private final int order;

	/**
	 * Creates an advisor at {@link #DEFAULT_ORDER}, which is guaranteed to run after the
	 * tool-search advisor. Prefer this unless you have a specific ordering need.
	 */
	public AvailableToolsLoggingAdvisor() {
		this(DEFAULT_ORDER);
	}

	/**
	 * Creates an advisor at an explicit chain order. The value must be greater than the
	 * tool-search advisor's order (see {@link #DEFAULT_ORDER}) for this logger to see the
	 * trimmed tool set.
	 */
	public AvailableToolsLoggingAdvisor(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
		List<String> toolNames = List.of();
		if (request.prompt().getOptions() instanceof ToolCallingChatOptions options
				&& options.getToolCallbacks() != null) {
			toolNames = options.getToolCallbacks()
				.stream()
				.map(tc -> tc.getToolDefinition().name())
				.sorted()
				.toList();
		}

		System.out.printf("%n%s>>> LLM call - %d tool(s) visible to the model: %s%s%n", CYAN,
				toolNames.size(), toolNames, RESET);

		// If the previous step was a tool response (e.g. tool-search results), show it.
		Message last = request.prompt().getLastUserOrToolResponseMessage();
		if (last instanceof ToolResponseMessage toolResponse) {
			toolResponse.getResponses()
				.forEach(r -> System.out.printf("%s      tool result [%s]: %s%s%n", DIM, r.name(),
						truncate(r.responseData(), 260), RESET));
		}
		return request;
	}

	@Override
	public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
		var chatResponse = response.chatResponse();
		if (chatResponse == null) {
			return response;
		}
		chatResponse.getResults()
			.stream()
			.map(Generation::getOutput)
			.filter(message -> !message.getToolCalls().isEmpty())
			.flatMap(message -> message.getToolCalls().stream())
			.forEach(toolCall -> System.out.printf("%s      model -> calls %s(%s)%s%n", GREEN, toolCall.name(),
					truncate(toolCall.arguments(), 160), RESET));
		return response;
	}

	private static String truncate(@Nullable String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() <= max ? text : text.substring(0, max) + "…";
	}

}