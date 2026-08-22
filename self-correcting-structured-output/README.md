# 🎯 talk-triage



Every LLM speaks one language: plain text. The moment your code needs to route on a
field, save a value, or branch on a result, that text has to become a typed object —
and the usual fix (a stricter prompt + fingers crossed 🤞) isn't engineering, it's hoping.

Spring AI 2.0 gives `ChatClient.entity(...)` two new dials to close that gap for real:
a **response-side safety net** that catches malformed output and asks the model to fix
it, and a **request-side constraint** that tells the provider to enforce the shape
up front. This project is a small conference-CFP triage API that demos both, live,
against a model small enough to fail on command. 😄

---

## 📚 Table of Contents

- [🧠 The Problem](#-the-problem)
- [🗂️ Project Layout](#️-project-layout)
- [🧩 The Domain: `TalkSubmission`](#-the-domain-talksubmission)
- [🚦 The Four Endpoints](#-the-four-endpoints)
- [🔀 Switching Model Providers](#-switching-model-providers)
- [🛠️ Running It](#️-running-it)
- [🔍 What's Actually Happening Under the Hood](#-whats-actually-happening-under-the-hood)
- [⚠️ Known Rough Edges](#️-known-rough-edges)
- [🧾 Cheat Sheet](#-cheat-sheet)
- [📖 References](#-references)

---

## 🧠 The Problem

`ChatClient` has supported typed responses since Spring AI's first release, via
`.call().entity(YourType.class)`. Behind that one line:

1. A **schema generator** turns your record into a JSON Schema.
2. That schema gets appended to the prompt as an instruction.
3. The model's text reply is handed to a **converter** that parses it back into your type.

That works — most of the time. But the model is only ever *asked* to follow the schema,
never *forced* to. A field goes missing, an enum value gets invented, the JSON arrives
wrapped in a friendly paragraph — and your deserializer blows up in production. 💥

Spring AI 2.0 adds two independent, opt-in switches to fix this without you writing a
single retry loop by hand:

| Switch | Fixes | Direction |
|---|---|---|
| `validateSchema()` | Catches bad output *after* the call, feeds the exact error back, retries | Response-side 🩹 |
| `useProviderStructuredOutput()` | Asks the provider's API to enforce the schema *before* it answers | Request-side 🔒 |

Both are **off by default** — existing code keeps behaving exactly as it always did.

---

## 🗂️ Project Layout

```
src/main/java/dev/danvega/talktriage/
├── TalkTriageApplication.java
├── config/
│   └── ChatClientConfig.java     # 🔧 the single ChatClient bean + system prompt
├── model/
│   └── TalkSubmission.java       # 📦 plain record — schema generated from its shape
└── controller/
    └── SubmissionController.java # 🚦 /typed, /validated, /combined, /schema

src/main/resources/
├── application.yml               # ⚙️ shared config, picks the active profile
├── application-ollama.yml        # 🦙 local model (llama3.2:1b) — the failure demo
├── application-nvidia.yml        # 🟩 NVIDIA NIM via the OpenAI-compatible client
└── application-anthropic.yml     # 🟠 Claude, via the native Anthropic starter

samples/messy-submission.txt      # ✉️ sample free-form CFP text
requests.http                     # 🧪 hits all four endpoints
```

---

## 🧩 The Domain: `TalkSubmission`

```java
public record TalkSubmission(
        String title,
        String talkAbstract,
        Level level,
        Track track,
        int durationMinutes,
        List<String> tags,
        String speakerHandle) {

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum Track { WEB, AI, DATA, CLOUD, SECURITY, LANGUAGES }
}
```

No annotations anywhere. 🎉 Spring AI reads the record's shape alone — property names,
types, enum values, which fields are required, and that no extra properties are
allowed — and generates a JSON Schema from it. That's the same schema
`validateSchema()` checks every response against, and it's what `/schema` returns raw
so you can show it side by side with the model's output.

---

## 🚦 The Four Endpoints

### 1️⃣ `POST /api/submissions/typed` — the old way, fingers crossed 🤞

```java
.call().entity(TalkSubmission.class);
```

The schema goes into the prompt and nothing checks what comes back. Point a small
model like `llama3.2:1b` at a messy CFP and watch it invent an enum value or wrap the
JSON in prose — this is the failure mode the rest of the video fixes.

### 2️⃣ `POST /api/submissions/validated` — self-correcting retries 🔁

```java
.call().entity(TalkSubmission.class, spec -> spec.validateSchema());
```

One extra call, and Spring AI wires in a `StructuredOutputValidationAdvisor` behind
the scenes:

1. The model answers.
2. Spring AI validates it against the `TalkSubmission` schema.
3. ✅ Valid → you get your typed record.
4. ❌ Invalid → the *specific* validation error (e.g. `"missing required field:
   speakerHandle"`) gets appended to the prompt, and the call is retried — up to
   **3 attempts by default**.

Turn on `DEBUG` logging for `StructuredOutputValidationAdvisor` (already set in
`application.yml`) and watch the failure, the fed-back error, and the corrected
retry happen in real time.

### 3️⃣ `POST /api/submissions/combined` — belt and suspenders 🩺🔒

```java
.call().entity(TalkSubmission.class,
    spec -> spec.useProviderStructuredOutput().validateSchema());
```

Adds the request-side constraint on top of the response-side retry: the provider is
told, at the API level, that its answer must conform to the schema — and Spring AI
still validates locally afterward. Fewer retries needed, maximum reliability.

### 4️⃣ `GET /api/submissions/schema` — show and tell 📄

Returns the raw JSON Schema Spring AI generated from `TalkSubmission`, so you can see
exactly what `validateSchema()` is checking against.

---

## 🔀 Switching Model Providers

Ollama, OpenAI (used for NVIDIA), and Anthropic starters are **all on the classpath
at once** — no pom.xml edits needed to switch. `spring.ai.model.chat` in each profile
decides which one wins the `ChatClient` bean, so there's no ambiguous-bean conflict.

```yaml
# application.yml
spring:
  profiles:
    active: ollama # or: nvidia, anthropic
```

Or override at launch without touching the file:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=nvidia
./mvnw spring-boot:run -Dspring-boot.run.profiles=anthropic
```

| Profile | Provider | Needs |
|---|---|---|
| 🦙 `ollama` | Local `llama3.2:1b` | `ollama pull llama3.2:1b` + Ollama running locally |
| 🟩 `nvidia` | NVIDIA NIM (OpenAI-compatible) | `NVIDIA_API_KEY` env var |
| 🟠 `anthropic` | Claude | `ANTHROPIC_API_KEY` env var |

---

## 🛠️ Running It

```bash
# 1. Pull the small local model used in the failure-mode demo
ollama pull llama3.2:1b

# 2. Run with the default (ollama) profile
./mvnw spring-boot:run

# 3. Fire the requests in requests.http (or use curl / IntelliJ's HTTP client)
```

---

## 🔍 What's Actually Happening Under the Hood

- **Schema generation** — `JsonSchemaGenerator.generateForType(...)` walks your record
  via reflection and builds a JSON Schema (2020-12 dialect) with no annotations required.
- **`validateSchema()`** registers a *recursive advisor*
  (`StructuredOutputValidationAdvisor`) — it can call the model again itself, feeding
  the validation error back as extra prompt context, up to a configurable number of
  attempts (default 3, customizable via `.maxRepeatAttempts(...)` if you build the
  advisor yourself instead of using the one-line switch).
- **`useProviderStructuredOutput()`** checks whether the active model's chat options
  implement `StructuredOutputChatOptions`. If they do, the schema is sent as an
  API-level field and the system prompt drops its JSON formatting instructions
  (cleaner prompt, fewer tokens). If the provider doesn't support it, the flag is
  silently ignored and the call falls back to the prompt-based default — so it's
  always safe to leave on.
- Both switches are **independent** — the response-side retry and the request-side
  constraint can be used alone or together, and neither changes behavior unless you
  explicitly opt in per call.

---

## ⚠️ Known Rough Edges

- 🧩 **Partial schema support.** Even providers that advertise native structured output
  often only support part of the JSON Schema spec — `$ref`, deep nesting,
  `allOf`/`anyOf`/`oneOf`, and recursive types are common gaps. This is exactly the
  shape-drift `validateSchema()` exists to catch.
- 🤔 **Reasoning models via Ollama** — "thinking" variants can emit their internal
  reasoning trace as plain text instead of JSON. Prefer a non-reasoning model, or pair
  native output with `validateSchema()` so the misfire gets retried automatically.
- 📋 **OpenAI rejects top-level arrays** in its structured-output API. If you need
  `List<TalkSubmission>` combined with `.useProviderStructuredOutput()` on OpenAI, wrap
  it: `record TalkSubmissions(List<TalkSubmission> talks) {}`. The default prompt-based
  path has no such restriction.
- 🌊 **No streaming support.** `.entity(...)` needs the complete response to parse, so
  it's `.call()`-only — `.stream()` still returns raw text chunks either way.

---

## 🧾 Cheat Sheet

| You need | Use |
|---|---|
| Default, works everywhere | `.entity(Type.class)` |
| Generic types (`List<T>`, `Map<K,V>`) | `.entity(new ParameterizedTypeReference<...>() {})` |
| Don't fail on malformed output | `.entity(Type.class, spec -> spec.validateSchema())` |
| Stronger upstream guarantee from the provider | `.entity(Type.class, spec -> spec.useProviderStructuredOutput())` |
| Both, maximum reliability | `.entity(Type.class, spec -> spec.useProviderStructuredOutput().validateSchema())` |
| Token usage / metadata alongside the entity | `.responseEntity(...)` (same overloads) |
| Model wraps JSON in markdown fences | Implement `StructuredOutputConverter<T>` |

---

## 📖 References

- [Spring blog: Self-Correcting Structured Output in Spring AI 2.0](https://spring.io/blog/2026/06/23/spring-ai-self-correcting-structured-output)
- [Structured Output Converter reference docs](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [ChatClient reference docs](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Recursive Advisors reference docs](https://docs.spring.io/spring-ai/reference/api/advisors-recursive.html)
