package com.omar.scso.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the single {@link ChatClient} bean shared by every endpoint in
 * {@code SubmissionController}.
 *
 * <p>Keeping this out of the controller means the controller only deals with
 * request/response mapping, and the system prompt / model wiring lives in one place
 * regardless of which chat model provider is active (Ollama or NVIDIA — see
 * {@code application-ollama.yml} / {@code application-nvidia.yml}).
 */
@Configuration
public class ChatClientConfig {

	private static final String SYSTEM_PROMPT = """
			You review conference talk submissions (CFPs) and extract them into structured data.
			The submissions are messy, free-form text written by speakers.
			Stay faithful to the speaker's original wording and details wherever possible.
			""";

	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.defaultSystem(SYSTEM_PROMPT).build();
	}

}
