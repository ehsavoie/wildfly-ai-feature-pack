/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mcp.api;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Designates a parameter of a {@code @Tool} method whose value clients must
 * mirror into an HTTP request header.
 * <p>
 * The designation is published in the tool's JSON Schema as the
 * {@code x-mcp-header} keyword on the parameter's property schema. When a
 * client calls the tool over Streamable HTTP transport, it sends the argument
 * value in the {@code Mcp-Param-<value>} header alongside the request body,
 * enabling intermediaries (gateways, load balancers) to route or inspect calls
 * without parsing the body.
 * </p>
 * <p>
 * This annotation is a supplement to
 * {@code @org.mcpjava.server.tools.ToolArg}: {@code @ToolArg} describes the
 * property (name, description, required status) while {@code @McpParamHeader}
 * adds the {@code x-mcp-header} keyword to that same property. Neither restates
 * the other's fields, so each field of a property's schema has exactly one
 * source.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Tool(name = "execute_sql", description = "Execute SQL on Google Cloud Spanner")
 * List<Row> executeSql(
 *     @ToolArg(description = "The region to execute the query in")
 *     @McpParamHeader("Region") String region,
 *     @ToolArg(description = "The SQL query to execute") String query) {
 *     ...
 * }
 * }</pre>
 *
 * <h2>Constraints</h2>
 * <ul>
 *   <li>{@link #value()} must not be empty.</li>
 *   <li>{@link #value()} must contain only ASCII characters, excluding space
 *       and {@code :}.</li>
 *   <li>{@link #value()} must be unique, ignoring case, among the parameters
 *       of a single tool.</li>
 *   <li>The annotated parameter must map to a primitive JSON Schema type:
 *       {@code integer}, {@code string}, or {@code boolean} &mdash;
 *       {@code number} is not permitted.</li>
 * </ul>
 *
 * @see org.mcpjava.server.tools.ToolArg
 */
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
public @interface McpParamHeader {

    /**
     * The header name carried by the {@code x-mcp-header} keyword.
     * <p>
     * Clients send the argument value in the {@code Mcp-Param-<value>}
     * request header.
     *
     * @return the header name
     */
    String value();
}
