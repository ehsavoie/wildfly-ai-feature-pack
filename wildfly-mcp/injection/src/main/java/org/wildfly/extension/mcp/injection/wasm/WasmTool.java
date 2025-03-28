/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp.injection.wasm;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Qualifier
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface WasmTool {

    public final class WasmToolLiteral extends AnnotationLiteral<WasmTool> implements WasmTool {

        /**
         * Default Singleton literal
         */
        public static final WasmToolLiteral INSTANCE = new WasmToolLiteral();

        private static final long serialVersionUID = 1L;

    }
}
