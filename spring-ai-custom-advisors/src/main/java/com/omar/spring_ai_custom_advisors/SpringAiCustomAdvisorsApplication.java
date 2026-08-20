package com.omar.spring_ai_custom_advisors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Entry point for the Spring AI Advisors demo.
 *
 * <p>This application showcases how the Spring AI {@code Advisor} API can be used as an
 * AOP-style interceptor around calls to a {@link org.springframework.ai.chat.client.ChatClient}.
 * Two custom advisors are wired up:
 *
 * <ul>
 *   <li>{@link com.omar.spring_ai_custom_advisors.advisor.AvailableToolsLoggingAdvisor} — logs which tools
 *       are visible to the model before a call, and which tools it actually invoked after.</li>
 *   <li>{@link com.omar.spring_ai_custom_advisors.advisor.TokenCounterAdvisor} — tracks prompt, completion,
 *       and total token usage per call, plus a running total across calls.</li>
 * </ul>
 */
@SpringBootApplication
public class SpringAiCustomAdvisorsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiCustomAdvisorsApplication.class, args);
	}

}
