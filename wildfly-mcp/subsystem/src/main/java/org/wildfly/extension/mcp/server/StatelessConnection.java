/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp.server;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.extension.mcp.api.ClientCapability;
import org.wildfly.extension.mcp.api.Implementation;
import org.wildfly.extension.mcp.api.InitializeRequest;
import org.wildfly.extension.mcp.api.MCPConnection;
import org.wildfly.extension.mcp.api.RequestMetadata;

public class StatelessConnection implements MCPConnection {

    static final String SYNTHETIC_CLIENT_NAME = "stateless-client";
    static final String SYNTHETIC_CLIENT_VERSION = "unknown";

    private final String id;
    private final RequestMetadata requestMetadata;
    private final InitializeRequest cachedInitializeRequest;
    private final PendingRequestRegistry pendingRequests = new PendingRequestRegistry();
    private volatile boolean cancelled;
    private final AtomicReference<Future<?>> runningTask = new AtomicReference<>();

    public StatelessConnection(RequestMetadata requestMetadata) {
        this.id = UUID.randomUUID().toString();
        this.requestMetadata = requestMetadata;
        if (requestMetadata != null) {
            List<ClientCapability> capabilities = requestMetadata.clientCapabilities() != null
                    ? requestMetadata.clientCapabilities()
                    : List.of();
            this.cachedInitializeRequest = new InitializeRequest(
                    new Implementation(SYNTHETIC_CLIENT_NAME, SYNTHETIC_CLIENT_VERSION),
                    requestMetadata.protocolVersion().wireValue(),
                    capabilities);
        } else {
            this.cachedInitializeRequest = null;
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Status status() {
        return Status.IN_OPERATION;
    }

    @Override
    public boolean initialize(InitializeRequest request) {
        // Stateless connections are always considered already initialized — no handshake is needed
        return false;
    }

    @Override
    public boolean setInitialized() {
        return false;
    }

    @Override
    public void task(Future<?> future) {
        if (!runningTask.compareAndSet(null, future) || cancelled) {
            future.cancel(true);  // already cancelled, or slot taken
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
        Future<?> task = runningTask.getAndSet(null);
        if (task != null) {
            task.cancel(true);
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    RequestMetadata requestMetadata() {
        return requestMetadata;
    }

    @Override
    public void close() {
    }

    @Override
    public PendingRequestRegistry pendingRequests() {
        return pendingRequests;
    }

    @Override
    public InitializeRequest initializeRequest() {
        return cachedInitializeRequest;
    }

    // Stateless connections are per-request and never registered in ConnectionManager,
    // so they are never subject to idle-timeout cleanup. Returns "now" to satisfy the interface.
    @Override
    public long lastActivity() {
        return System.currentTimeMillis();
    }
}
