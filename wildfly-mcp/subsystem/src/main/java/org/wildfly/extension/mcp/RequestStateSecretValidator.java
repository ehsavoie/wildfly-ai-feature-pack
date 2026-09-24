/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp;

import java.util.Base64;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates that {@code request-state-secret} is valid Base64 and decodes to at least 16 bytes,
 * matching the minimum key length required by {@link org.wildfly.extension.mcp.server.RequestStateCodec}.
 */
class RequestStateSecretValidator implements ParameterValidator {

    private static final int MIN_DECODED_LENGTH = 16;

    static final RequestStateSecretValidator INSTANCE = new RequestStateSecretValidator();

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        if (!value.isDefined() || value.getType() != ModelType.STRING) {
            return;
        }
        String encoded = value.asString();
        if (encoded.isEmpty() || MCPSubsystemRegistrar.UNDEFINED_SENTINEL.equals(encoded)) {
            return;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new OperationFailedException(MCPLogger.ROOT_LOGGER.invalidRequestStateSecret(e).getMessage());
        }
        if (decoded.length < MIN_DECODED_LENGTH) {
            throw new OperationFailedException(MCPLogger.ROOT_LOGGER.secretTooShort().getMessage());
        }
    }
}
