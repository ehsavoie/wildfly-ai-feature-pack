/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.wasm.api;

public interface WasmInvoker {

    public byte[] call(String method, byte[] input);
}
