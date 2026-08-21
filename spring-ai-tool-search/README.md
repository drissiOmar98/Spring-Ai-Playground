# Spring AI Tool Search Demo

**Give an LLM 50 tools without giving it 50 tools' worth of tokens.**

Every `@Tool` you register gets its JSON schema — name, description, parameters — serialized into
*every* request, before a user has typed a single word. This project makes that hidden "tool tax"
visible with a live before/after comparison, then shows how Spring AI 2.0's **Tool Search Tool**
(`ToolSearchToolCallingAdvisor`) removes it by letting the model *search* for the tools it needs
instead of receiving all of them upfront.
---

## Table of Contents

- [The Problem](#the-problem)
- [How Tool Search Solves It](#how-tool-search-solves-it)
  - [Runtime Flow](#runtime-flow)
  - [Session Scoping](#session-scoping)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
  - [Default Profile — Anthropic](#default-profile--anthropic)
  - [`nvidia` Profile — NVIDIA NIM](#nvidia-profile--nvidia-nim)
  - [Search Strategies](#search-strategies)
  - [Full Property Reference](#full-property-reference)
- [Running the Application](#running-the-application)
- [Endpoints](#endpoints)
- [Results: Before vs. After](#results-before-vs-after)
- [Testing with the `.http` File](#testing-with-the-http-file)
- [When to Use Tool Search](#when-to-use-tool-search)
- [Index Eviction](#index-eviction)
- [Try It Yourself](#try-it-yourself)
- [Further Reading](#further-reading)

---

## The Problem

As agents connect to more systems — Slack, GitHub, Jira, internal microservices, MCP servers —
their tool catalogs grow fast. A moderately connected agent can easily accumulate 50+ tools,
and sending all of their schemas on every single call adds up in two ways:

- **Cost** — thousands of prompt tokens spent on tool definitions the model will never call in
  that turn.
- **Accuracy** — models get measurably worse at picking the *right* tool once the list grows
  past a few dozen similarly-named options.

This repo demonstrates the effect directly: the same question, asked three ways, with the prompt
token count printed to the console for each.

## How Tool Search Solves It

`ToolSearchToolCallingAdvisor` implements a pattern called **progressive tool disclosure**: tool
definitions are revealed to the model incrementally, on demand, rather than all at once. Instead
of shipping every schema up front, the model is given a single lightweight `toolSearchTool` and
uses it to query for whatever capability the current turn actually requires.

![Tool Search Tool Calling Flow](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/_images/tools/spring-ai-tool-search-tool-calling-flow.png)

### Runtime Flow

1. **Indexing** — at session start, every registered tool is indexed into a `ToolIndex`. Nothing
   is sent to the model yet.
2. **Initial request** — the first call to the LLM includes only the built-in `toolSearchTool`
   definition — a single, tiny schema regardless of how many real tools exist.
3. **Discovery call** — when the model decides it needs a capability, it invokes `toolSearchTool`
   with a natural-language description of what it's looking for.
4. **Search & expand** — the configured `ToolIndex` matches that query against the indexed
   catalog and returns the closest tools; their real definitions are appended to the conversation.
5. **Tool invocation** — now equipped with the actual schema, the model issues a normal tool call.
6. **Execution** — the tool runs as usual and its result is returned to the model.
7. **Response** — the model answers using the tool's output.

The net effect: the prompt only ever carries the handful of tool definitions relevant to the
current conversation, no matter how large the underlying catalog grows.

### Session Scoping

Tool indexes are built **per session**, not globally — this is what lets multiple concurrent
conversations stay isolated from one another. The advisor reads a session identifier from its
context on every request, by default under the same key `ChatMemory.CONVERSATION_ID` uses. If
your app already threads a memory advisor with a conversation ID through the chain, tool-search
session scoping comes for free from that same key.

Without a session ID, the very first tool-search call fails fast rather than silently mixing
sessions together — which is why every endpoint below that uses tools sets one explicitly.

## Project Structure

```
com.example.toolsearchdemo
├── ToolSearchDemoApplication.java
├── config/
│   ├── ChatClientConfig.java     # builds the ChatClient bean
│   └── VectorStoreConfig.java    # in-memory VectorStore backing the semantic ToolIndex
├── advisor/
│   ├── AvailableToolsLoggingAdvisor.java   # logs which tools reach the model per call
│   └── TokenCounterAdvisor.java            # logs prompt/completion/running token totals
├── tool/
│   └── InitechTools.java         # 47 @Tool methods (6 real + 41 filler, for a realistic haystack)
└── controller/
    └── ChatController.java       # /before, /after, /tool-search
```

## Prerequisites

- Java 24+
- Spring Boot 4.1+
- An Anthropic API key (default profile) and/or an NVIDIA API key (`nvidia` profile)

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>  <!-- also serves the NVIDIA profile -->
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-tool-search-advisor</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-simple</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-transformers</artifactId>  <!-- local embedding model -->
</dependency>
```

> Spring AI 2.0 no longer auto-activates a chat provider just because its starter is on the
> classpath. With two model starters present (Anthropic + OpenAI-compatible), the active one is
> selected explicitly via `spring.ai.model.chat` in configuration — see below.

## Configuration

### Default Profile — Anthropic

`src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: tool-search
  threads:
    virtual:
      enabled: true
  ai:
    model:
      chat: anthropic   # explicit provider selection
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        model: claude-opus-4-8
    chat:
      client:
        tool-search-advisor:
          enabled: true
          tool-index-type: vector
          max-results: 3
          reference-tool-name-accumulation: true
```

### `nvidia` Profile — NVIDIA NIM

`src/main/resources/application-nvidia.yaml`

NVIDIA is reached through Spring AI's OpenAI-compatible client pointed at NVIDIA's endpoint.
Their API requires `max-tokens` to be set explicitly, or it returns a server error.

```yaml
spring:
  ai:
    model:
      chat: openai
    openai:
      api-key: ${NVIDIA_API_KEY}
      base-url: https://integrate.api.nvidia.com
      chat:
        options:
          model: meta/llama-3.1-70b-instruct
          max-tokens: 2048
    chat:
      client:
        tool-search-advisor:
          enabled: true
          tool-index-type: vector
          max-results: 3
          reference-tool-name-accumulation: true
```

Run with it active:

```bash
export NVIDIA_API_KEY=nvapi-xxxxxxxxxxxxxxxxxxxxxx
./mvnw spring-boot:run -Dspring-boot.run.profiles=nvidia
```

The token-savings behavior is provider-agnostic — the same before/after comparison holds
whether the model behind the tool-search advisor is Claude or a NVIDIA-hosted Llama model.

### Search Strategies

| Strategy     | Implementation    | Best for                                                                   | Requires                  |
| ------------ | ------------------ | --------------------------------------------------------------------------- | -------------------------- |
| **Semantic** | `VectorToolIndex`  | Natural-language queries, fuzzy matching, novel phrasings                   | a `VectorStore` bean       |
| **Keyword**  | `LuceneToolIndex`  | Exact-term matching, fast retrieval, known vocabulary                       | Lucene on the classpath (bundled in the starter) |
| **Regex**    | `RegexToolIndex`   | Tool names following a strict naming convention (e.g. `get_*_data`)         | nothing — zero dependencies, and the default |

This project uses `vector` so the model can find tools by describing what it needs in plain
language, rather than having to know exact tool-name vocabulary.

### Full Property Reference

| Property                                                                     | Description                                                                   | Default                       |
| ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------ |
| `spring.ai.chat.client.tool-search-advisor.enabled`                          | Turns the advisor on, replacing the default `ToolCallingAdvisor`.               | `false`                        |
| `spring.ai.chat.client.tool-search-advisor.tool-index-type`                  | `regex`, `lucene`, or `vector`.                                                 | `regex`                        |
| `spring.ai.chat.client.tool-search-advisor.max-results`                      | Max tool references returned per search call.                                  | `null` (model decides, ~5)     |
| `spring.ai.chat.client.tool-search-advisor.reference-tool-name-accumulation` | Accumulate tools discovered across all search turns vs. keep only the latest.  | `true`                         |
| `spring.ai.chat.client.tool-search-advisor.session-id-key-name`              | Context key the advisor reads the session ID from.                             | `chat_memory_conversation_id`  |
| `spring.ai.chat.client.tool-search-advisor.lucene.min-score-threshold`       | Minimum score for a Lucene hit to count (when using `lucene`).                 | `0.25`                         |
| `spring.ai.chat.client.tool-search-advisor.eviction.lru-max-sessions`        | Active sessions retained before LRU eviction kicks in.                         | `1000`                         |
| `spring.ai.chat.client.tool-search-advisor.eviction.ttl`                     | Idle-session TTL (e.g. `30m`, `1h`).                                            | `null`                         |

## Running the Application

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./mvnw spring-boot:run
```

The first run also pulls a local embedding model for the vector index, so give it a minute.

## Endpoints

| Endpoint       | Tools sent to the model                          | Purpose                                        |
| -------------- | -------------------------------------------------- | ------------------------------------------------ |
| `GET /before`      | None                                            | Baseline — cost of the bare prompt              |
| `GET /after`        | All 47, every request                           | The "naive" approach — the tool tax made visible |
| `GET /tool-search`  | Only what the model searches for, per turn      | Progressive disclosure via the Tool Search advisor |

All three accept an optional `message` query parameter (defaults to *"What is tomorrow's
date?"*).

## Results: Before vs. After

Same question, three strategies:

| Endpoint       | Tools visible to the model | Prompt tokens |
| -------------- | --------------------------- | -------------- |
| `/before`      | 0                            | 17             |
| `/after`       | 47                           | 5,111          |
| `/tool-search` | 1 (`toolSearchTool`) → expands to matches | ~1,166 |

Loading all 47 tools costs roughly **300x** the bare-prompt baseline in tokens. Tool search
recovers most of that — around a **5x reduction** versus loading everything — while still giving
the model access to the full 47-tool catalog when it actually needs something from it.

`AvailableToolsLoggingAdvisor` and `TokenCounterAdvisor` print this live to the console on every
call, so you can watch the tool list and token counts shrink and grow in real time as you swap
endpoints.

## Testing with the `.http` File

`requests.http` (IntelliJ HTTP Client format):

```http
### Variables
@baseUrl = http://localhost:8080
@question = What is tomorrow's date?

### Baseline — no tools registered
GET {{baseUrl}}/before?message={{question}}

### Naive approach — all 47 tools loaded into every request
GET {{baseUrl}}/after?message={{question}}

### Tool Search — dynamic tool discovery via vector search
GET {{baseUrl}}/tool-search?message={{question}}

### Multistep task — needs several distinct tools
GET {{baseUrl}}/tool-search?message=Employee Jane Doe (E-4471) is flying from Austin to Berlin next Monday. Look up her profile, book her a flight, reserve her a desk, and tell her what to pack. Use a tool for today's date.
```

Each `###` block runs independently in the IDE, showing status, headers, timing, and body inline
— handy for eyeballing token differences call by call.

## When to Use Tool Search

**Good fit:**
- 10+ tools registered on the `ChatClient`.
- Tool schemas alone are consuming 10K+ tokens per request.
- A multi-server MCP setup where the combined catalog is large.
- You're seeing tool-selection mistakes because the model is choosing between too many similarly
  named options.

**Stick with the default `ToolCallingAdvisor` when:**
- Your tool library is small (under 10 tools).
- Nearly every tool is used in nearly every session anyway.
- Tool schemas are already tiny — the extra search round-trip could cost more than it saves.

## Index Eviction

Per-session tool indexes live in memory, so something has to reclaim them. Five strategies ship
out of the box:

| Strategy                        | Behavior                                                                 |
| -------------------------------- | --------------------------------------------------------------------------- |
| `LruEvictionStrategy` (default) | Evicts the least-recently-used session once a configurable cap is exceeded (1000 by default). |
| `NeverEvictStrategy`            | Keeps indexes forever until `evictSession()` is called explicitly.        |
| `AlwaysEvictStrategy`           | Rebuilds the index on every single request — useful for testing.          |
| `TtlEvictionStrategy`           | Evicts sessions idle longer than a given duration.                        |
| `CompositeEvictionStrategy`     | Combines multiple strategies — evicts if any of them says to.             |

Eviction is checked lazily on each request, so there's no background thread to manage.

## Try It Yourself

- **Swap the index type.** Change `tool-index-type` to `lucene` or `regex` and compare which
  tools surface for the same query.
- **Grow the catalog.** Add more `@Tool` methods to `InitechTools` — the index picks them up
  automatically and prompt size doesn't grow with it.
- **Route to a different domain.** Try *"open a P1 incident and page on-call"* and watch search
  surface the DevOps/Support tools instead of the travel ones used in the example above.
- **Switch providers.** Run the same comparison against the `nvidia` profile and confirm the
  token-savings ratio holds regardless of which model is behind the advisor.

## Further Reading

- [Spring AI — Tool Search Tool reference](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools/tool-search-tool.html)
- [Spring AI — Tool Calling: Scaling to Hundreds of Tools](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools.html#scaling-to-hundreds-of-tools)
- [Spring AI — Dynamic Tool Discovery guide](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/guides/dynamic-tool-search.html)
- [Spring blog — Smart Tool Selection: 34–64% Token Savings (Dec 2025)](https://spring.io/blog/2025/12/11/spring-ai-tool-search-tools-tzolov)
