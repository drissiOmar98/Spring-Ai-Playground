package com.omar.spring_ai_custom_advisors.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prints per-call and running-total token usage — the proof of what tool schemas cost on
 * every request, and what tool search saves.
 */
public class TokenCounterAdvisor implements BaseAdvisor {

	private static final String PURPLE = "\u001B[35m";

	private static final String RESET = "\u001B[0m";

	private final AtomicInteger requests = new AtomicInteger();

	private final AtomicInteger promptTokens = new AtomicInteger();

	private final AtomicInteger completionTokens = new AtomicInteger();

	private final AtomicInteger totalTokens = new AtomicInteger();

	@Override
	public int getOrder() {
		// Last in the chain, so it sees each iteration's usage.
		return Ordered.LOWEST_PRECEDENCE - 1000;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
		return request;
	}

	@Override
	public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
		var chatResponse = response.chatResponse();
		if (chatResponse == null) {
			return response;
		}
		var usage = chatResponse.getMetadata().getUsage();
		this.requests.incrementAndGet();
		this.promptTokens.addAndGet(usage.getPromptTokens());
		this.completionTokens.addAndGet(usage.getCompletionTokens());
		this.totalTokens.addAndGet(usage.getTotalTokens());

		System.out.printf(
				"%s      tokens this call: prompt=%d completion=%d total=%d  |  running total over %d call(s): %d%s%n",
				PURPLE, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens(),
				this.requests.get(), this.totalTokens.get(), RESET);
		return response;
	}

}