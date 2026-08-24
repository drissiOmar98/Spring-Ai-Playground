# 🧳 Travel Chat — Live Tool-Calling with Spring AI 2.0

> See every tool call happen in real time — just like Claude and ChatGPT's "Calling tool..." indicator — built from scratch in a Spring Boot app.

A travel-planning chat assistant that streams its **answer tokens** and **tool-call lifecycle events** (`STARTED` → `COMPLETED`) to the browser over Server-Sent Events, using Spring AI 2.0's new `ToolCallingAdvisor`.

---

## 📚 Table of Contents

- [✨ Overview](#-overview)
- [🎥 What This Demo Covers](#-what-this-demo-covers)
- [🛠️ Tools Available to the Model](#️-tools-available-to-the-model)
- [🏗️ Architecture](#️-architecture)
- [📁 Project Structure](#-project-structure)
- [⚙️ Prerequisites](#️-prerequisites)
- [🚀 Getting Started](#-getting-started)
- [🔌 API Reference](#-api-reference)
- [📡 SSE Event Format](#-sse-event-format)
- [🧠 Why Spring AI 2.0 Changes This](#-why-spring-ai-20-changes-this)
- [🗺️ Roadmap](#️-roadmap)
- [📄 License](#-license)

---

## ✨ Overview

Ask the assistant something like:

> "I'm planning a trip to Chicago next week — what should I pack, and roughly what will flights cost?"

The model resolves *"next week"* with `getCurrentDate`, pulls a forecast with `getWeatherForecast`, checks `getPackingSuggestions`, and estimates cost with `estimateFlightPrice` — and the browser sees each call **start** and **complete** live, before the final answer streams in.

## 🎥 What This Demo Covers

- 🔄 What changed with tool calling in **Spring AI 2.0** vs. the per-model loops in 1.x
- 🧩 How `ToolCallingAdvisor` becomes a first-class, composable part of the advisor chain
- 🏷️ Defining tools with `@Tool` and wiring them into a `ChatClient`
- 👀 Writing a custom **observing advisor** that intercepts intermediate tool-call events
- 📡 Streaming tool-call events to a front end with `Flux` and Server-Sent Events

## 🛠️ Tools Available to the Model

| Tool | Description |
|---|---|
| 📅 `getCurrentDate` | Resolves relative dates like "next week" or "tomorrow" |
| 🌦️ `getWeatherForecast` | Forecast for a city over a date range |
| 🎫 `getCityEvents` | Notable events happening in a city over a date range |
| 💱 `convertCurrency` | Converts an amount between two currencies |
| 🎒 `getPackingSuggestions` | What to pack for a city and date range |
| 🕒 `getLocalTime` | Current local time and UTC offset for a city |
| ✈️ `estimateFlightPrice` | Rough round-trip flight price between two cities |
| 🏨 `estimateAccommodationCost` | Rough nightly lodging cost by style (budget/mid-range/luxury) |
| 🛂 `getVisaRequirements` | Visa and entry requirements by nationality and destination |
| 🚇 `getPublicTransitOptions` | Local transit options and typical fares |
| 🍽️ `getRestaurantRecommendations` | Restaurant suggestions, optionally by cuisine |

> ℹ️ All tools currently return canned/illustrative data (see Javadoc in `TravelTools`) — swap the method bodies for real API calls without touching the `@Tool` contracts.

## 🏗️ Architecture

```
Browser (SSE client)
      │  POST /api/chat
      ▼
ChatController ──uses──▶ ChatClient (configured in ChatClientConfig)
      │                          │
      │                          ├─ MessageChatMemoryAdvisor  (outside tool loop)
      │                          ├─ ToolCallingAdvisor        (Spring AI 2.0, implicit)
      │                          └─ ToolCallObservingAdvisor  (inside tool loop, per-request)
      │                                       │
      │                                       ▼ emits STARTED / COMPLETED
      ▼
ChatEventStream ──merges──▶  token events + tool events ──▶ SSE response
```

- `MessageChatMemoryAdvisor` runs **outside** the tool loop (`HIGHEST_PRECEDENCE + 200`) — it only ever sees one user message and one final answer per turn.
- `ToolCallObservingAdvisor` runs **inside** the tool loop (`ToolCallingAdvisor.DEFAULT_ORDER + 100`) — it sees every intermediate tool call the loop would otherwise hide.

## 📁 Project Structure

```
src/main/java/com/example/travelchat/
├── TravelChatApplication.java        # @SpringBootApplication, main()
├── config/
│   └── ChatClientConfig.java         # ChatClient bean (advisors, memory)
├── tools/
│   └── TravelTools.java              # @Tool methods exposed to the model
├── events/
│   ├── ToolCallEvent.java            # STARTED/COMPLETED record
│   ├── ToolCallObservingAdvisor.java # observes the tool-calling loop
│   └── ChatEventStream.java          # per-turn SSE plumbing
└── web/
    └── ChatController.java           # POST /api/chat (SSE)
```

## ⚙️ Prerequisites

- ☕ Java 21+
- 🧰 Maven or Gradle
- 🔑 An OpenAI (or other Spring AI-supported) API key
- 🌱 Spring Boot 4.1 + Spring AI 2.0

## 🚀 Getting Started

1. **Clone and configure**
   ```bash
   git clone <your-repo-url>
   cd travel-chat
   export OPENAI_API_KEY=sk-...
   ```

2. **Set your key in `application.yml`**
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPENAI_API_KEY}
   ```

3. **Run it**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Try it**
   ```bash
   curl -N -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"message": "Plan a 3-day trip to Chicago next week", "conversationId": "demo-1"}'
   ```

## 🔌 API Reference

### `POST /api/chat`

**Request body**

| Field | Type | Required | Description |
|---|---|---|---|
| `message` | string | ✅ | The user's chat message |
| `conversationId` | string | ❌ | Groups turns into one memory thread (defaults to `"default"`) |

**Response** — `text/event-stream` (see below)

## 📡 SSE Event Format

| Event | Payload | Meaning |
|---|---|---|
| `token` | JSON-encoded string | One chunk of the assistant's streamed answer |
| `tool` | JSON `ToolCallEvent` (`id`, `name`, `phase`) | A tool call started or completed |
| `error` | plain string | An upstream error occurred; stream still closes cleanly |
| `done` | *(empty)* | Terminal event — the turn is finished |

Example stream:

```
event: tool
data: {"id":"call_1","name":"getCurrentDate","phase":"STARTED"}

event: tool
data: {"id":"call_1","name":"getCurrentDate","phase":"COMPLETED"}

event: token
data: "Here's"

event: token
data: " your itinerary..."

event: done
data:
```

## 🧠 Why Spring AI 2.0 Changes This

In Spring AI 1.x, the tool-calling loop lived inside each model's `ChatModel` implementation — there was no clean seam to observe intermediate calls without reaching into model-specific internals. Spring AI 2.0 lifts the loop into `ToolCallingAdvisor`, a regular, ordered part of the advisor chain — so a plain `CallAdvisor`/`StreamAdvisor` placed *closer to the model* than `ToolCallingAdvisor` can observe every iteration for free, which is exactly what `ToolCallObservingAdvisor` does here.

## 🗺️ Roadmap

- [ ] Swap canned tool data for real weather/events/flight APIs
- [ ] Add a minimal HTML/JS SSE client under `src/main/resources/static`
- [ ] Persist `ChatMemory` beyond in-process (e.g. JDBC-backed)
- [ ] Add tool-level error events (`tool` phase `FAILED`)

## 📄 License

MIT — use freely for your own demos and projects.
