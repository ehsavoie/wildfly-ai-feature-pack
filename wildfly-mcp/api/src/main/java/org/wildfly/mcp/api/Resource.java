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
 * Annotates a business method of a CDI bean as an exposed resource.
 * <p>
 * The result of a "resource read" operation is always represented as a {@link ResourceResponse}. However, the annotated method
 * can also return other types that are converted according to the following rules.
 *
 * <ul>
 * <li>If the method returns an implementation of {@link ResourceContents} then the reponse contains the single contents
 * object.</li>
 * <li>If the method returns a {@link List} of {@link ResourceContents} implementations then the reponse contains the list of
 * contents objects.</li>
 * <li>The method may return a {@link Uni} that wraps any of the type mentioned above.</li>
 * </ul>
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Resource {

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * "A human-readable name for this resource."
     */
    String name() default ELEMENT_NAME;

    /**
     * "A description of what this resource represents."
     */
    String description() default "";

    /**
     * "The URI of this resource."
     */
    String uri();

    /**
     * "The MIME type of this resource, if known."
     */
    String mimeType() default "";

}
