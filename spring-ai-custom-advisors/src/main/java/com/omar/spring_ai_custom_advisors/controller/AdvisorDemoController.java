package com.omar.spring_ai_custom_advisors.controller;


import com.omar.spring_ai_custom_advisors.advisor.AvailableToolsLoggingAdvisor;
import com.omar.spring_ai_custom_advisors.advisor.EmojiLoggingAdvisor;
import com.omar.spring_ai_custom_advisors.advisor.TokenCounterAdvisor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 🎛️ Demo endpoints, one per advisor, so each can be exercised in isolation before
 * seeing them stacked together in {@link #combined}.
 *
 * <p>Every endpoint accepts an optional {@code question} param and falls back to a
 * Spring / Spring AI question when omitted, so a bare {@code curl} or browser hit is
 * enough to see each advisor in action.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdvisorDemoController {

	private static final String DEFAULT_QUESTION = "In Spring Boot, what's the practical difference between "
			+ "constructor injection and field injection, and why is constructor injection preferred?";

	private final ChatClient chatClient;


	/**
	 * Demonstrates {@link AvailableToolsLoggingAdvisor}: watch the server console to see
	 * which tools were visible to the model and which one(s) it actually called.
	 *
	 * @param question the question to ask; defaults to a Spring Boot DI question
	 * @return the model's answer
	 */
	@GetMapping("/api/advisors/tools-logging")
	public String toolsLogging(@RequestParam(defaultValue = DEFAULT_QUESTION) String question) {
		log.info("[CONTROLLER] tools-logging demo - question='{}'", question);
		return chatClient.prompt()
			.advisors(new AvailableToolsLoggingAdvisor())
			.user(question)
			.call()
			.content();
	}

	/**
	 * Demonstrates {@link TokenCounterAdvisor}: each call adds to a running total you
	 * can inspect via {tokenUsage}.
	 *
	 * @param question the question to ask; defaults to a Spring Boot DI question
	 * @return the model's answer
	 */
	@GetMapping("/api/advisors/token-counter")
	public String tokenCounter(@RequestParam(defaultValue = DEFAULT_QUESTION) String question) {
		log.info("[CONTROLLER] token-counter demo - question='{}'", question);
		return chatClient
			.prompt()
			.advisors(new TokenCounterAdvisor())
			.user(question)
			.call()
			.content();
	}


	/**
	 * Demonstrates {@link EmojiLoggingAdvisor}: check the application's DEBUG logs for
	 * the raw request/response dump.
	 *
	 * @param question the question to ask; defaults to a Spring Boot DI question
	 * @return the model's answer
	 */
	@GetMapping("/api/advisors/emoji-logging")
	public String emojiLogging(@RequestParam(defaultValue = DEFAULT_QUESTION) String question) {
		log.info("[CONTROLLER] emoji-logging demo - question='{}'", question);
		return chatClient
			.prompt()
			.advisors(new EmojiLoggingAdvisor())
			.user(question)
			.call()
			.content();
	}

	/**
	 * Stacks all three advisors on a single call, so the console shows tool visibility,
	 * token counts, and the raw debug dump together — closest to a real production
	 * observability setup.
	 *
	 * @param question the question to ask; defaults to a Spring Boot DI question
	 * @return the model's answer
	 */
	@GetMapping("/api/advisors/combined")
	public String combined(@RequestParam(defaultValue = DEFAULT_QUESTION) String question) {
		log.info("[CONTROLLER] combined advisors demo - question='{}'", question);
		return chatClient
			.prompt()
			.advisors(new AvailableToolsLoggingAdvisor(), new TokenCounterAdvisor(), new EmojiLoggingAdvisor())
			.user(question)
			.call()
			.content();
	}

}
