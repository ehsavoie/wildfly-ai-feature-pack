/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mcp.api.wasm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a field/parameter method of a CDI bean as an WASM Resource tool.
 */
@Retention(RUNTIME)
@Target(value={FIELD})
public @interface WasmTool {

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Each tool must have a unique name. By default, the name is derived from the name of the annotated field/parameter.
     */
    String name() default ELEMENT_NAME;


}
