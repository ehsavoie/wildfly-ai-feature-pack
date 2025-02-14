/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mcp.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Annotates a business method of a CDI bean as an exposed tool.
 * <p>
 * The result of a "tool call" operation is always represented as a {@link ToolResponse}. However, the annotated method can also
 * return other types that are converted according to the following rules.
 *
 * <ul>
 * <li>If the method returns {@link String} then the reponse is {@code success} and contains the single {@link TextContent}
 * object.</li>
 * <li>If the method returns an implementation of {@link Content} then the reponse is {@code success} and contains the single
 * content object.</li>
 * <li>If the method returns a {@link List} of {@link Content} implementations or {@link String}s then the reponse is
 * {@code success} and contains the list of relevant content objects.</li>
 * <li>The method may return a {@link Uni} that wraps any of the type mentioned above.</li>
 * </ul>
 *
 * @see ToolResponse
 * @see ToolArg
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Tool {

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Each tool must have a unique name. By default, the name is derived from the name of the annotated method.
     */
    String name() default ELEMENT_NAME;

    /**
     * An optional description.
     */
    String description() default "";

}
