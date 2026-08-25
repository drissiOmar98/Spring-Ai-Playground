package com.omar.spring_ai_guardrails.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables {@link GuardrailProperties} so it can be injected wherever the
 * demo needs its word lists.
 *
 * <p>There is deliberately no shared, pre-built {@code ChatClient} bean here.
 * Each controller builds its own client from the injected
 * {@link org.springframework.ai.chat.client.ChatClient.Builder} and attaches
 * a different combination of advisors, because the whole point of the demo
 * is to show each layer in isolation before stacking them.
 */
@Configuration
@EnableConfigurationProperties(GuardrailProperties.class)
public class GuardrailsConfig {
}
