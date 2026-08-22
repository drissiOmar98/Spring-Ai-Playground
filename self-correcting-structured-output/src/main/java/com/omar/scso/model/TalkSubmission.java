package com.omar.scso.model;

import java.util.List;

/**
 * A structured conference talk submission, extracted from a messy free-form CFP.
 *
 * <p>Just a plain Java record — no annotations. Spring AI generates a JSON Schema from
 * the record's shape alone: property names, types, enum values, required fields, and no
 * additional properties allowed. That generated schema is what {@code validateSchema()}
 * checks the model's output against.
 */
public record TalkSubmission(
		String title,
		String talkAbstract,
		Level level,
		Track track,
		int durationMinutes,
		List<String> tags,
		String speakerHandle) {

	public enum Level {
		BEGINNER, INTERMEDIATE, ADVANCED
	}

	public enum Track {
		WEB, AI, DATA, CLOUD, SECURITY, LANGUAGES
	}

}
