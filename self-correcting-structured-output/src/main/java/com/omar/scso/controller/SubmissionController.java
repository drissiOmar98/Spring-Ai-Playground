package com.omar.scso.controller;


import com.omar.scso.model.TalkSubmission;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Three ways to turn a messy, free-form CFP submission into a typed
 * {@link TalkSubmission}, from least to most reliable.
 *
 * <p>The {@link ChatClient} used here is a shared bean configured in
 * {@code config.ChatClientConfig} — this controller only maps requests to responses.
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

	private final ChatClient chatClient;

	public SubmissionController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Demo 1: Typed — the way we've always done it. Spring AI appends the JSON schema
	 * to the prompt and hopes the model complies. No validation, no retries: if the
	 * model rambles, wraps the JSON in prose, or invents an enum value, deserialization
	 * blows up (or worse — silently succeeds with out-of-spec data).
	 */
	@PostMapping("/typed")
	public TalkSubmission typed(@RequestBody String rawSubmission) {
		return this.chatClient.prompt()
			// .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT) // not supported on every model
			.user(u -> u.text("Extract this talk submission:\n\n{submission}").param("submission", rawSubmission))
			.call()
			.entity(TalkSubmission.class);
	}

	/**
	 * Demo 2: Self-correcting structured output (NEW in Spring AI 2.0). The response is
	 * validated against the generated JSON schema. On failure, the specific validation
	 * error is appended to the prompt and the model gets another shot (up to 3 retries
	 * by default). Watch the logs: you'll see the failure, the error fed back, and the
	 * corrected second attempt.
	 */
	@PostMapping("/validated")
	public TalkSubmission validated(@RequestBody String rawSubmission) {
		return this.chatClient.prompt()
			.user(u -> u.text("Extract this talk submission:\n\n{submission}").param("submission", rawSubmission))
			.call()
			.entity(TalkSubmission.class, spec -> spec.validateSchema());
	}

	/**
	 * Demo 3: Belt and suspenders. The schema is ALSO enforced by the provider's native
	 * structured output API (request side), and the response is still validated locally
	 * (response side). Fewer retries needed, maximum reliability.
	 */
	@PostMapping( "/combined")
	public TalkSubmission combined(@RequestBody String rawSubmission) {
		return this.chatClient.prompt()
			.user(u -> u.text("Extract this talk submission:\n\n{submission}").param("submission", rawSubmission))
			.call()
			.entity(TalkSubmission.class, spec -> spec.useProviderStructuredOutput().validateSchema());
	}

	/**
	 * Show-and-tell: the exact JSON schema Spring AI generates from the
	 * {@link TalkSubmission} record. The same schema {@code validateSchema()} enforces.
	 */
	@GetMapping( "/schema")
	public String schema() {
		return JsonSchemaGenerator.generateForType(TalkSubmission.class);
	}

}
