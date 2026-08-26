package com.omar.spring_ai_guardrails.controller;


import com.omar.spring_ai_guardrails.advisor.PiiMaskingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Layer 4b: {@link PiiMaskingAdvisor}, and the advisor-ordering gotcha that
 * goes with it.
 *
 * <p>Both endpoints here send the exact same message. The only difference is
 * the order the two advisors are registered in. Watch the application log
 * while calling each one - it's the clearest way to see why order in the
 * advisor chain isn't cosmetic.
 */
@RestController
public class PiiMaskingController {

    private static final String MESSAGE_WITH_PII =
            "My name is Omar (dr.omar98@yahoo.com) and my card 4111 1111 1111 1111 "
                    + "was double charged, can you help me?";

    private final ChatClient chatClient;

    public PiiMaskingController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Correct order: {@code PiiMaskingAdvisor} runs before
     * {@code SimpleLoggerAdvisor} (its {@code getOrder()} of 100 is lower
     * than the logger's 200). The request the logger sees - and logs - is
     * already masked, so the console never shows the raw email or card
     * number.
     */
    @GetMapping("/api/pii/masked-before-log")
    public String maskedBeforeLog(@RequestParam(defaultValue = MESSAGE_WITH_PII) String message) {
        return chatClient.prompt()
                .advisors(new PiiMaskingAdvisor(), new SimpleLoggerAdvisor(200))
                .user(message)
                .call()
                .content();
    }

    /**
     * Wrong order, on purpose: passing the logger to {@code .advisors(...)}
     * before the masking advisor does <b>not</b> actually control execution
     * order - Spring AI always runs advisors by {@code getOrder()}, lowest
     * first, regardless of the sequence they're listed in. To really put
     * the logger first, its order value has to be lower than the masking
     * advisor's. That's what this endpoint does: {@code new
     * SimpleLoggerAdvisor(50)} runs ahead of {@code PiiMaskingAdvisor}'s
     * order of 100, so the logger captures the request before masking ever
     * touches it. The console shows the raw email and card number even
     * though the caller still gets a clean, masked response. That gap
     * between "the response is safe" and "the log is safe" is exactly why
     * order needs to be checked deliberately, not assumed from how the
     * advisors happen to be listed.
     */
    @GetMapping("/api/pii/logger-runs-first")
    public String loggerRunsFirst(@RequestParam(defaultValue = MESSAGE_WITH_PII) String message) {
        SimpleLoggerAdvisor earlyLogger = new SimpleLoggerAdvisor(50);

        return chatClient.prompt()
                .advisors(earlyLogger, new PiiMaskingAdvisor())
                .user(message)
                .call()
                .content();
    }
}
