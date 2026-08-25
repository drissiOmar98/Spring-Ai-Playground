package com.omar.spring_ai_guardrails.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Centralizes the word lists and secrets used by the guardrail advisors and
 * demo controllers so the "what are we protecting?" list lives in one place
 * instead of being scattered across advisor constructors.
 *
 * <p>Bound from the {@code guardrails.*} prefix in {@code application.yml},
 * with sensible defaults so the app still runs if the config is omitted.
 *
 * @param sensitiveWords words passed to Spring AI's built-in
 *                        {@link org.springframework.ai.chat.client.advisor.SafeGuardAdvisor}.
 *                        Matching is a case-sensitive substring check, which
 *                        is exactly the weakness this demo exploits.
 * @param secrets         values that must never appear in a model response,
 *                        checked by {@link com.omar.spring_ai_guardrails.advisor.SecretLeakAdvisor}.
 * @param blockedPhrases  phrases/topics the {@link com.omar.spring_ai_guardrails.advisor.CustomInputRailAdvisor}
 *                        rejects after normalizing the input (lower-cased,
 *                        whitespace collapsed), which is what lets it catch
 *                        the spacing and case tricks that beat a plain
 *                        substring filter.
 */
@ConfigurationProperties(prefix = "guardrails")
public record GuardrailProperties(
        List<String> sensitiveWords,
        List<String> secrets,
        List<String> blockedPhrases
) {

    public GuardrailProperties {
        if (sensitiveWords == null) {
            sensitiveWords = List.of("pass");
        }
        if (secrets == null) {
            secrets = List.of("SUNRISE50");
        }
        if (blockedPhrases == null) {
            blockedPhrases = List.of(
                    "password",
                    "admin credential",
                    "secret key",
                    "ignore previous instructions",
                    "ignore your instructions",
                    "disregard the system prompt",
                    "you are now"
            );
        }
    }
}
