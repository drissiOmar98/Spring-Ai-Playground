package com.omar.spring_ai_custom_advisors.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ⚙️ Central {@link ChatClient} wiring for the demo.
 *
 * <p>Exposes a single pre-configured {@link ChatClient.Builder} with the
 * so every controller endpoint gets
 * the same baseline setup and only needs to attach the advisor(s) it wants to
 * demonstrate.
 */
@Configuration
public class ChatClientConfig {


	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.build();
	}


}
