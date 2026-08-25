package com.omar.spring_ai_guardrails.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.core.Ordered;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input-side guardrail that catches what {@link org.springframework.ai.chat.client.advisor.SafeGuardAdvisor}'s
 * case-sensitive substring match lets through.
 *
 * <p>{@code SafeGuardAdvisor} checks whether the raw user text contains one
 * of its sensitive words, verbatim. That's trivial to beat: a different
 * case ({@code PASSWORD}), inserted spaces or punctuation ({@code p a s s
 * w o r d}), or a synonym the list doesn't happen to contain all sail
 * straight through.
 *
 * <p>This advisor closes those specific gaps by normalizing the text before
 * matching:
 * <ol>
 *   <li>Unicode normalization (folds accented / look-alike characters).</li>
 *   <li>Lower-casing.</li>
 *   <li>Collapsing whitespace and stripping punctuation between letters, so
 *       {@code "p a-s-s w o r d"} normalizes to {@code "password"}.</li>
 * </ol>
 * It then checks the normalized text against a list of blocked phrases,
 * which can include multi-word prompt-injection patterns
 * ({@code "ignore previous instructions"}) that a single-word list can't
 * express at all.
 *
 * <p>This is still a blocklist, and blocklists are never complete - the
 * point of the demo is that no single layer is enough, not that this one is
 * unbeatable.
 */
public class CustomInputRailAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(CustomInputRailAdvisor.class);

    /** Matches any run of characters that isn't a letter or digit, used to strip spacing/punctuation tricks. */
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final List<String> blockedPhrases;
    private final String failureResponse;

    public CustomInputRailAdvisor(List<String> blockedPhrases, String failureResponse) {
        this.blockedPhrases = blockedPhrases;
        this.failureResponse = failureResponse;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = request.prompt().getUserMessage().getText();
        String normalized = normalize(userText);

        boolean blocked = blockedPhrases.stream()
                .map(this::normalize)
                .anyMatch(normalized::contains);

        if (blocked) {
            log.warn("Custom input rail blocked a request after normalization");
            return refuse();
        }

        return chain.nextCall(request);
    }

    /**
     * Normalizes text for matching: Unicode-normalizes, lower-cases, then
     * strips all non-alphanumeric characters so that spacing, punctuation,
     * and simple obfuscation between letters can't be used to slip past the
     * blocklist.
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String unicodeNormalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return NON_ALPHANUMERIC.matcher(unicodeNormalized.toLowerCase()).replaceAll("");
    }

    private ChatClientResponse refuse() {
        ChatResponse refusal = new ChatResponse(
                List.of(new Generation(new AssistantMessage(failureResponse))));
        return ChatClientResponse.builder()
                .chatResponse(refusal)
                .build();
    }

    @Override
    public String getName() {
        return "CustomInputRailAdvisor";
    }

    @Override
    public int getOrder() {
        // Runs before SafeGuardAdvisor's default position so a request that
        // passes both layers really did pass both, not just this one.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
