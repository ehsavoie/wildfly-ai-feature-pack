/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mcp.api;

import static org.wildfly.mcp.api._private.MCPApiLogger.ROOT_LOGGER;

public class MissingCapabilityException extends RuntimeException {

    private final String capability;

    public MissingCapabilityException(String capability) {
        super(ROOT_LOGGER.missingClientCapability(capability));
        this.capability = capability;
    }

    public String capability() {
        return capability;
    }
}
