---
title: MCP Overview
description: Model Context Protocol support -- MCP server and client capabilities in the WildFly AI Feature Pack.
layout: page
---

# Model Context Protocol (MCP)

The feature pack provides comprehensive support for the [Model Context Protocol (MCP)](https://spec.modelcontextprotocol.io/), both as a client and as a server.

## MCP Server

The `mcp-server` Galleon layer lets you expose your Jakarta EE application as an MCP server. Annotated CDI bean methods become MCP **tools**, **prompts**, and **resources** accessible over JSON-RPC 2.0 / Streamable HTTP.

### Setup

Add the MCP server annotation API as a provided dependency:

```xml
<dependency>
    <groupId>org.mcp-java</groupId>
    <artifactId>mcp-server-api</artifactId>
    <scope>provided</scope>
</dependency>
```

> **Note (0.10.0+):** The annotation API has moved from the `wildfly-mcp/api` module to [`org.mcp-java:mcp-server-api`](https://github.com/mcp-java/java-mcp-annotations) for standardized annotations across runtimes. Update your imports to `org.mcp_java.server.*`.

### Defining Tools

Annotate methods with `@Tool` and parameters with `@ToolArg`:

```java
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

@Tool(name = "weather", description = "Gets the weather for a city")
String getWeather(@ToolArg(description = "City name") String city) {
    return fetchWeather(city);
}
```

### Defining Prompts

```java
import org.mcpjava.server.prompts.Prompt;
import org.mcpjava.server.prompts.PromptArg;

@Prompt(name = "summarize", description = "Summarizes a document")
PromptResponse summarize(@PromptArg(description = "The document text") String text) {
    return PromptResponse.of(Role.USER,
        TextContent.of("Please summarize: " + text));
}
```

### Defining Resources

```java
import org.mcpjava.server.resources.Resource;

@Resource(uri = "config://app", name = "App Config",
          description = "Application configuration")
String getConfig() {
    return loadConfig();
}
```

### Supported Transports

The MCP server exposes a single `/mcp` endpoint that automatically negotiates the protocol version based on the `MCP-Protocol-Version` header sent by the client. All three spec versions are handled by the same endpoint:

- **Streamable HTTP** (all versions) -- POST to `/mcp`
- **Server-Sent Events (SSE)** -- Legacy SSE transport, used when the client does not send a protocol version header

## MCP Protocol Versions

The server supports three MCP spec versions on a single endpoint. The version is negotiated per-connection using the `MCP-Protocol-Version` request header during `initialize`.

| Version | Key additions |
|---------|---------------|
| `2025-03-26` | Baseline: tools, prompts, resources, SSE transport |
| `2025-11-25` | Streamable HTTP transport, pagination, cancellation |
| `2026-07-28` | `server/discover`, `subscriptions/listen`, stateless connections, cache metadata |

Clients that do not send a version header are handled using the legacy SSE transport (equivalent to `2025-03-26`). Clients that request an unsupported version receive an error listing the supported versions.

## Service Discovery (`server/discover`)

Introduced in `2026-07-28`. A client can call `server/discover` at any point -- before, during, or after the `initialize` handshake -- to retrieve the server's supported protocol versions, capabilities, and cache hints without committing to a session.

The response contains:

| Field | Description |
|-------|-------------|
| `supportedVersions` | Array of spec version strings the server accepts |
| `capabilities` | Same capabilities object returned by `initialize` |
| `serverInfo` | `{ name, version }` of the server |
| `_meta.ttlMs` | Suggested time-to-live in milliseconds for caching this response |
| `_meta.cacheScope` | `"public"` or `"private"` cache directive |

`server/discover` is also available to stateless connections (see [Stateless Connections](#stateless-connections)).

## List-Changed Notifications

The server can push `notifications/tools/list_changed`, `notifications/prompts/list_changed`, and `notifications/resources/list_changed` to all connected clients whenever the set of available tools, prompts, or resources changes at runtime.

Inject `org.wildfly.mcp.api.ListChangeNotifier` as a tool method parameter (the framework resolves it automatically and it does not appear in the tool's input schema), then call the appropriate method:

```java
import org.wildfly.mcp.api.ListChangeNotifier;

@Tool(name = "reload_tools", description = "Reloads the tool registry and notifies clients")
String reloadTools(ListChangeNotifier notifier) {
    // ... reload logic ...
    notifier.notifyToolsChanged();
    return "Tools reloaded";
}
```

| Method | Notification sent |
|--------|-------------------|
| `notifyToolsChanged()` | `notifications/tools/list_changed` |
| `notifyPromptsChanged()` | `notifications/prompts/list_changed` |
| `notifyResourcesChanged()` | `notifications/resources/list_changed` |

## Resource Subscriptions

Introduced in `2026-07-28`. Clients can call `subscriptions/listen` with a list of `{ type, uri }` targets to receive `notifications/resources/updated` when those resources change. Each `subscriptions/listen` call **replaces** the client's entire subscription set -- it is not additive. Sending an empty list clears all subscriptions.

The standard `resources/subscribe` and `resources/unsubscribe` methods (from earlier spec versions) are also supported for per-resource management.

> **Note:** `notifications/message` (logging notifications) are never delivered on subscription streams.

## Cache Metadata

The `server/discover` response includes `_meta.ttlMs` and `_meta.cacheScope` to help clients cache discovery results and reduce round-trips. Configure these values on the MCP subsystem resource:

| Attribute | Default | Description |
|-----------|---------|-------------|
| `cache-ttl` | `3600000` ms (1 hour) | How long clients may cache the discover response |
| `cache-scope` | `public` | `public` (shared caches allowed) or `private` (per-client only) |

```bash
/subsystem=mcp:write-attribute(name=cache-ttl, value=300000)
/subsystem=mcp:write-attribute(name=cache-scope, value=private)
```

## Stateless Connections

Introduced in `2026-07-28`. A stateless connection is a per-request connection that skips the `initialize` / `initialized` handshake. The server treats the connection as already in the `IN_OPERATION` state, deriving client capabilities and protocol version from the `MCP-Protocol-Version` and related headers on each request.

Stateless connections are suitable for:
- Clients calling `server/discover` without establishing a session
- Serverless or short-lived runtimes that cannot maintain persistent connections
- Intermediaries forwarding isolated requests

Stateless connections are not registered in the connection manager and are not subject to idle-timeout cleanup. Each request creates an independent connection with a unique ID.

## MCP Client

The feature pack can connect to external MCP servers as a client.

| Layer | Transport |
|-------|-----------|
| `mcp-client-sse` | Server-Sent Events |
| `mcp-client-stdio` | Standard I/O |
| `mcp-client-streamable` | Streamable HTTP |

### SSE Client Configuration

```bash
export MCP_CLIENT_HOST=localhost
export MCP_CLIENT_PORT=8080
export MCP_CLIENT_SSE_PATH=/mcp
export MCP_CLIENT_LOG_REQUEST=true
export MCP_CLIENT_LOG_RESPONSE=true
```

## Securing the MCP Server

Bearer token authentication via OIDC is handled by the `elytron-oidc-client` subsystem. Configure with Keycloak:

1. Start Keycloak:

```bash
podman run -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.2.1 start-dev
```

2. Create a realm and client following the [WildFly OIDC guide](https://www.wildfly.org/guides/security-oidc-management-console).

3. Add OIDC login to your `web.xml`:

```xml
<login-config>
    <auth-method>OIDC</auth-method>
</login-config>
```

4. Configure the `elytron-oidc-client` subsystem:

```bash
/subsystem=elytron-oidc-client/secure-deployment=ROOT.war:add(\
  client-id=mcp-client, \
  bearer-only=true, \
  provider-url="$\{env.OIDC_PROVIDER_URL:http://localhost:8080}/realms/myrealm", \
  ssl-required=EXTERNAL, \
  public-client="true", \
  principal-attribute="preferred_username")
```

The secured deployment **must** be configured with `bearer-only=true` so the MCP server authenticates using the bearer token provided by the MCP client.

## Examples

- [wildfly-weather](https://github.com/ehsavoie/wildfly-weather) -- MCP server example
- [wildfly-mcp-chatbot](https://github.com/wildfly-extras/wildfly-mcp/tree/main/wildfly-chat-bot) -- MCP client chatbot
- [webchat](https://github.com/ehsavoie/webchat/) -- Complete AI chat application

## Next Steps

- [Tool Development](mcp-tools) -- Advanced MCP tool APIs (headers, capabilities, multi-round)
- [Security](security) -- Security architecture and deployment hardening
