/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp.injection.wasm;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import java.util.Map;
import org.wildfly.extension.mcp.injection.MCPLogger;
import org.wildfly.extension.mcp.injection.WildFlyWasmRegistry;
import org.wildfly.mcp.api.wasm.WasmInvoker;

public class WasmPortableExtension implements Extension {

    private final WildFlyWasmRegistry registry;
    private final ClassLoader deploymentClassLoader;

    public WasmPortableExtension(WildFlyWasmRegistry registry, ClassLoader deploymentClassLoader) {
        this.registry = registry;
        this.deploymentClassLoader = deploymentClassLoader;
    }

    public void atd(@Observes AfterBeanDiscovery atd) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        for (Map.Entry<String, WasmToolConfiguration> entry : registry.listWasmTools().entrySet()) {
            atd.addBean()
                    .scope(ApplicationScoped.class)
                    .addQualifier(NamedLiteral.of(entry.getKey()))
                    .types(WasmInvoker.class)
                    .produceWith(c -> entry.getValue().create());
            MCPLogger.ROOT_LOGGER.info(entry.getKey() + " should be discoverable by CDI as a WASM Tool");
        }
    }
}
