/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp.injection.wasm;

import java.util.Map;
import org.extism.sdk.chicory.Manifest;
import org.extism.sdk.chicory.ManifestWasm;
import org.extism.sdk.chicory.Plugin;
import org.wildfly.mcp.api.wasm.WasmInvoker;

public class WasmToolConfiguration {
    private final String name;
    private final String wasmFile;
    private final Map<String, String> config;


    public WasmToolConfiguration(String name, String wasmFile, Map<String, String> config) {
        this.name = name;
        this.wasmFile = wasmFile;
        this.config = config;
    }

    public String name() {
        return name;
    }

    public WasmInvoker create() {
        ManifestWasm wasm = ManifestWasm.fromUrl(wasmFile).build();
        Manifest manifest = Manifest.ofWasms(wasm)
                .withOptions(new Manifest.Options().withConfig(config)).build();
        final Plugin plugin = Plugin.ofManifest(manifest).build();
        return (String method, byte[] input) -> plugin.call(method, input);
    }
}
