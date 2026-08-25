package com.omar.spring_ai_guardrails.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Locale;

/**
 * Output-side guardrail. Lets the model answer, then scans the response for
 * secrets before it reaches the caller.
 *
 * <p>An input filter has to anticipate every way a user might <i>ask</i> for
 * something - infinite surface area. This advisor only has to check whether
 * the answer contains the one string that must never leave, which is a much
 * smaller problem. That's why it's a useful backstop even when an input rail
 * is also in place: the {@code /leak} demo shows a discount code getting
 * past a naive input filter (spelled out letter by letter) and still getting
 * caught here, because by then the model has echoed it back in full.
 */
public class SecretLeakAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SecretLeakAdvisor.class);

    private final List<String> secrets;

    public SecretLeakAdvisor(List<String> secrets) {
        this.secrets = secrets;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        String reply = response.chatResponse().getResult().getOutput().getText();

        boolean leaked = reply != null && secrets.stream()
                .anyMatch(secret -> reply.toLowerCase(Locale.ROOT).contains(secret.toLowerCase(Locale.ROOT)));

        if (!leaked) {
            return response;
        }

        log.warn("Secret found in model response, replacing it before it reaches the caller");
        ChatResponse safe = new ChatResponse(
                List.of(new Generation(new AssistantMessage("I'm not able to share that."))));
        return ChatClientResponse.builder()
                .chatResponse(safe)
                .context(response.context())
                .build();
    }

    @Override
    public String getName() {
        return "SecretLeakAdvisor";
    }

    @Override
    public int getOrder() {
        // Sits between the input filter (near HIGHEST_PRECEDENCE) and the
        // SimpleLoggerAdvisor (200): the logger is closer to the model call,
        // so the console still shows the raw leak for the demo, while this
        // advisor redacts the response on its way back out to the caller.
        return 100;
    }
}
