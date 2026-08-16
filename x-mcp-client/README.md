# xmcp — X MCP Servers × Spring Boot 4.1 × Spring AI 2.0

A Spring AI 2.0 MCP client that connects Claude to X's (formerly Twitter) two
hosted [Model Context Protocol](https://modelcontextprotocol.io) servers over
remote Streamable HTTP, and lets the model decide — via tool calling — which
one to use for a given question.

## 🧠 What are the X MCP servers?

X exposes two hosted MCP servers so any MCP-compatible client can talk to the
platform without hand-rolling REST integrations, token refresh, or rate-limit
handling:

| Server | Endpoint | Purpose | Auth |
|---|---|---|---|
| **X API Docs** | `https://docs.x.com/mcp` | Search/read X's official API documentation | None |
| **X API (live)** | `https://api.x.com/mcp` | Search posts, look up users, and other live X API operations, exposed as MCP tools | Bearer token |

This matters for the same reason MCP matters generally: instead of a
one-off integration per AI tool, X ships the protocol once and any
MCP-compatible client — Claude, Cursor, Grok, this project — gets the same
tool surface for free.

## 🏗️ Architecture

```
                 ┌─────────────────────────┐
   HTTP request  │   Spring Boot 4.1 app   │
  ──────────────▶│                         │
                 │  DocsController         │
                 │  XApiController          │
                 │         │               │
                 │         ▼               │
                 │     ChatClient          │◀── Anthropic Claude (Spring AI)
                 │   (Spring AI 2.0)        │
                 └─────────┬───────┬───────┘
                           │       │
              Streamable   │       │  Streamable HTTP
              HTTP (no auth)      (Bearer token)
                           │       │
                           ▼       ▼
              ┌─────────────┐   ┌─────────────┐
              │ docs.x.com  │   │  api.x.com  │
              │    /mcp     │   │    /mcp     │
              └─────────────┘   └─────────────┘
```

**MCP communication flow:**

1. On startup, Spring AI's MCP client auto-configuration opens a Streamable
   HTTP connection to each server declared under
   `spring.ai.mcp.client.streamable-http.connections` and calls `tools/list`
   on each, aggregating the results into a single `ToolCallbackProvider`.
2. That provider is registered as the default tool set on the app's
   `ChatClient` bean.
3. A controller sends a user question to the `ChatClient`. Claude inspects
   the available tools' names/descriptions/schemas and decides — per the MCP
   *tool calling* pattern — whether the docs tools, the live API tools, or
   none of them are relevant.
4. If a tool call is issued, Spring AI's `DefaultToolCallingManager` executes
   it against the matching MCP connection and feeds the tool result back to
   Claude to produce the final answer.

## 🔐 Authentication

- `x-docs` (`docs.x.com/mcp`) requires no authentication.
- `x-api` (`api.x.com/mcp`) is authenticated with a bearer token, attached
  per-request via a `McpClientCustomizer<HttpClientStreamableHttpTransport.Builder>`
  bean scoped to that connection only, so the token is never sent to
  `docs.x.com`. The token itself is read from the `X_BEARER_TOKEN`
  environment variable — it is never written into `application.yaml` or
  committed to source control.

  > ⚠️ X's own documentation describes `api.x.com/mcp` as requiring OAuth 2.0
  > + PKCE via X's `xurl` CLI bridge rather than a static bearer header sent
  > directly to `api.x.com`. This project uses the simpler bearer-header
  > approach as a demo; if the live API connection returns `401`, that's this
  > gap surfacing, and the fix is running `xurl` locally and pointing the
  > `x-api` connection at its loopback endpoint instead. See
  > `XApiAuthConfig`'s Javadoc for the full explanation.

## 🤖 Spring AI + Claude

The chat model is Anthropic Claude, wired in through
`spring-ai-starter-model-anthropic`. A single `ChatClient` bean
(`ChatClientConfig`) is built once, with the MCP-derived
`ToolCallbackProvider` set as its default tools — every prompt sent through
it automatically has both MCP servers' tools available, no per-request
wiring.

## 🚀 Demos

### 1. 📚 X API Documentation Assistant — `GET /docs`

Sends a fixed question about the X API to the `ChatClient`. Claude calls the
`x-docs` MCP server's search/read tools to ground its answer in the official
documentation rather than its own training data.

### 2. 🔎 Real-Time X Search — `GET /search?topic=...`

Sends a topic to the `ChatClient`. Claude calls the `x-api` MCP server's
search tool to pull recent posts and summarizes the themes it finds — a
live, tool-grounded answer rather than a static one.

## ⚙️ Configuration

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
    mcp:
      client:
        streamable-http:
          connections:
            x-docs:
              url: https://docs.x.com
            x-api:
              url: https://api.x.com
```

Required environment variables — see `.env.example`:

- `ANTHROPIC_API_KEY`
- `X_BEARER_TOKEN` (only needed for `/search`)

```bash
mvn spring-boot:run
curl localhost:8080/docs
curl "localhost:8080/search?topic=Spring%20AI"
```

## Project layout

```
src/main/java/com/drissiomar/xmcp/
├── XmcpApplication.java        # entry point
├── config/
│   ├── ChatClientConfig.java   # ChatClient bean wired with MCP tool callbacks
│   └── XApiAuthConfig.java     # bearer auth for the x-api connection only
└── web/
    ├── DocsController.java     # GET /docs
    └── XApiController.java     # GET /search
```

## Verify before deploying anywhere real

- `spring-boot-starter-parent` and `spring-ai-bom` versions in `pom.xml` —
  pin to whatever's current on [start.spring.io](https://start.spring.io).
- The exact `McpClientCustomizer` API surface if you're pinned to a
  different Spring AI version than 2.0.
