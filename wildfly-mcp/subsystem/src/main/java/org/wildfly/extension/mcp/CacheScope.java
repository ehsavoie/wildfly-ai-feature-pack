/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp;

public enum CacheScope {
    PUBLIC("public"), PRIVATE("private");
    private final String value;

    CacheScope(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static CacheScope fromString(String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (PUBLIC.value.equalsIgnoreCase(string)) {
            return PUBLIC;
        }
        if (PRIVATE.value.equalsIgnoreCase(string)) {
            return PRIVATE;
        }
        throw new IllegalArgumentException();
    }
}
