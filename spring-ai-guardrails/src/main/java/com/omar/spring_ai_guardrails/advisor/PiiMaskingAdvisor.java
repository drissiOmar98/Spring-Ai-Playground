package com.omar.spring_ai_guardrails.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.regex.Pattern;

/**
 * Masks personally identifiable information in the outbound user message
 * before it leaves the process - i.e. before it's sent to the model
 * provider's API.
 *
 * <p>This is a data-protection concern as much as a safety one: even a
 * well-behaved model call still means a third party's API sees whatever you
 * hand it. Masking here means a card number, SSN, email, or phone number a
 * user pastes into a support chat never actually reaches OpenAI (or whichever
 * provider is configured).
 *
 * <p><b>Advisor order matters here.</b> This advisor must run with a lower
 * {@link #getOrder()} value (higher precedence) than
 * {@code SimpleLoggerAdvisor}. {@code SimpleLoggerAdvisor} logs whatever
 * request it sees at the point it runs in the chain - if it runs before this
 * advisor, the log line captures the raw, unmasked PII, defeating the point.
 * The {@code /pii/unmasked-log} endpoint in {@code PiiMaskingController}
 * deliberately reverses the order to show exactly that failure on camera.
 */
public class PiiMaskingAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(PiiMaskingAdvisor.class);

    /** 13-16 digit card numbers, optionally grouped with spaces or dashes. */
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b");

    /** US Social Security Numbers in NNN-NN-NNNN form. */
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    /** Standard email address shape. */
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");

    /** US-style phone numbers, with or without a leading country code. */
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?1[ .-]?)?\\(?\\d{3}\\)?[ .-]?\\d{3}[ .-]?\\d{4}\\b");

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String original = request.prompt().getUserMessage().getText();
        String masked = mask(original);

        if (!masked.equals(original)) {
            log.info("PII masked before outbound call");
        }

        ChatClientRequest updated = request.mutate()
                .prompt(request.prompt().augmentUserMessage(masked))
                .build();

        return chain.nextCall(updated);
    }

    private String mask(String text) {
        text = CARD.matcher(text).replaceAll("[CARD REDACTED]");
        text = SSN.matcher(text).replaceAll("[SSN REDACTED]");
        text = EMAIL.matcher(text).replaceAll("[EMAIL REDACTED]");
        text = PHONE.matcher(text).replaceAll("[PHONE REDACTED]");
        return text;
    }

    @Override
    public String getName() {
        return "PiiMaskingAdvisor";
    }

    @Override
    public int getOrder() {
        // Lower number = higher precedence = runs closer to the caller, so
        // masking happens before SimpleLoggerAdvisor (order 200) ever sees
        // the request.
        return 100;
    }
}
