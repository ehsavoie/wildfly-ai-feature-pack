/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp;

import org.wildfly.extension.mcp.injection.wasm.WasmToolConfiguration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.wildfly.extension.mcp.Capabilities.WASM_TOOL_PROVIDER_CAPABILITY;
import static org.wildfly.extension.mcp.WasmToolProviderRegistrar.METHOD_NAME;
import static org.wildfly.extension.mcp.WasmToolProviderRegistrar.TOOL_ARGUMENTS;
import static org.wildfly.extension.mcp.WasmToolProviderRegistrar.TOOL_DESCRIPTION;
import static org.wildfly.extension.mcp.WasmToolProviderRegistrar.WASM_PATH;
import static org.wildfly.extension.mcp.WasmToolProviderRegistrar.WASM_RELATIVE_TO;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.mcp.injection.tool.ArgumentMetadata;
import org.wildfly.extension.mcp.injection.tool.McpFeatureMetadata;
import org.wildfly.extension.mcp.injection.tool.MethodMetadata;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

public class WasmToolProviderServiceConfigurator implements ResourceServiceConfigurator {

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        final String path = WASM_PATH.resolveModelAttribute(context, model).asString();
        final String relativeTo = WASM_RELATIVE_TO.resolveModelAttribute(context, model).asStringOrNull();
        String methodName = METHOD_NAME.resolveModelAttribute(context, model).asStringOrNull();
        if (methodName == null) {
            methodName = context.getCurrentAddressValue();
        }
        final String description = TOOL_DESCRIPTION.resolveModelAttribute(context, model).asStringOrNull();
        ServiceDependency<PathManager> pathManager = ServiceDependency.on(PathManager.SERVICE_DESCRIPTOR);
        List<ArgumentMetadata> arguments;
        if (model.hasDefined(TOOL_ARGUMENTS.getName())) {
            arguments = new ArrayList<>();
            for (final ModelNode argumentModel : model.get(TOOL_ARGUMENTS.getName()).asList()) {
                try {
                    arguments.add(new ArgumentMetadata(argumentModel.get(NAME).asString(), argumentModel.get(DESCRIPTION).asString(), argumentModel.get(REQUIRED).asBoolean(false), Class.forName(argumentModel.get(TYPE).asString())));
                } catch (ClassNotFoundException ex) {
                    throw new OperationFailedException(ex);
                }
            }
        } else {
            arguments = Collections.emptyList();
        }
        MethodMetadata methodMetadata = new MethodMetadata(methodName, description, null, null, arguments, InvocationHandler.class.getName(), relativeTo);
        McpFeatureMetadata metadata = new McpFeatureMetadata(McpFeatureMetadata.Kind.TOOL, context.getCurrentAddressValue(), methodMetadata);
        Supplier<WasmToolConfiguration> factory = new Supplier<>() {
            @Override
            public WasmToolConfiguration get() {
                try {
                    String wasmFile = new File(pathManager.get().resolveRelativePathEntry(path, relativeTo)).toURI().toURL().toString();
                    return new WasmToolConfiguration(wasmFile, Collections.emptyMap());
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        return CapabilityServiceInstaller.builder(WASM_TOOL_PROVIDER_CAPABILITY, factory)
                .asActive()
                .build();
    }

}
