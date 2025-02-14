package org.wildfly.mcp.api;

/**
 *
 * @param role The sender or recipient of messages and data in a conversation.
 * @param content
 *
 * @see TextContent
 * @see ImageContent
 * @see ResourceContent
 */
public record PromptMessage(String role, Content content) {

    public static PromptMessage withUserRole(Content content) {
        return new PromptMessage("user", content);
    }

}
