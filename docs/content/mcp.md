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

The MCP server supports:
- **Streamable HTTP** -- POST to the `/mcp` endpoint
- **Server-Sent Events (SSE)** -- Legacy SSE transport

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
