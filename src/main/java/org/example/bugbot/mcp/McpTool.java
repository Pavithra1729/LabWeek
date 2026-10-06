package org.example.bugbot.mcp;

import java.util.Map;
import java.util.function.Function;

/**
 * Describes a single MCP tool: its name, human description, JSON-schema for
 * its input, and the handler that actually executes it.
 *
 * @param name        unique tool id, e.g. "jira.search"
 * @param description what the tool does (shown to the LLM)
 * @param inputSchema JSON Schema object describing the arguments
 * @param handler     executes the tool given the parsed arguments
 */
public record McpTool(
        String name,
        String description,
        Map<String, Object> inputSchema,
        Function<Map<String, Object>, Object> handler
) {}

