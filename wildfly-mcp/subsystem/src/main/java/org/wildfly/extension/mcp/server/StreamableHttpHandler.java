package org.wildfly.extension.mcp.server;

import static io.undertow.util.Headers.ALLOW;

import static org.wildfly.extension.mcp.api.ConnectionManager.MCP_SESSION_ID_HEADER;
import static org.wildfly.extension.mcp.server.McpStreamableConnectionCallBack.JSON_PAYLOAD;
import static org.wildfly.extension.mcp.server.McpStreamableConnectionCallBack.SESSION_ID;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.wildfly.extension.mcp.api.ConnectionManager;
import org.wildfly.extension.mcp.api.JsonRPC;
import org.wildfly.extension.mcp.MCPLogger;
import org.wildfly.extension.mcp.injection.WildFlyMCPRegistry;


public class StreamableHttpHandler implements HttpHandler {
    private final ConnectionManager connectionManager;
    static McpMessageHandler handler;
    private final HttpHandler sseHandler;

    public StreamableHttpHandler(ConnectionManager connectionManager, WildFlyMCPRegistry registry, ClassLoader classLoader, 
            String serverName, String applicationName, HttpHandler sseHandler) {
        this.connectionManager = connectionManager;
        this.sseHandler = sseHandler;
        handler = new McpMessageHandler(connectionManager,registry, classLoader, serverName, applicationName);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(Methods.GET.equals(exchange.getRequestMethod())) {
            this.sseHandler.handleRequest(exchange);
            return;
        }
        if(! Methods.POST.equals(exchange.getRequestMethod())) {
            exchange.setStatusCode(405).getResponseHeaders().add(ALLOW, Methods.POST_STRING);
            exchange.endExchange();
            return;
        }
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.setPersistent(true);
        exchange.startBlocking();
        JsonReader reader = Json.createReader(exchange.getInputStream());
        JsonObject content = reader.readObject();
        MCPLogger.ROOT_LOGGER.debug("Received message from client: %s".formatted(content));
        String connectionId = exchange.getRequestHeaders().getFirst(MCP_SESSION_ID_HEADER);
        if (connectionId == null) {
            connectionId = connectionManager.id();
            exchange.putAttachment(SESSION_ID, connectionId);
            exchange.putAttachment(JSON_PAYLOAD, content);
            exchange.setStatusCode(202);
            exchange.getResponseHeaders().put(MCP_SESSION_ID_HEADER, connectionId);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream");
            exchange.getResponseHeaders().put(Headers.CONNECTION, "keep-alive");
            this.sseHandler.handleRequest(exchange);
            MCPLogger.ROOT_LOGGER.debugf("Streamable connection initialized [%s]");
            return;
        }
        ServerSentEventResponder connection = (ServerSentEventResponder)connectionManager.get(connectionId);
        JsonRPC.validate(content, connection);
        handler.handle(content, connection, connection);
    }


}
