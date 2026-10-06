package org.example.bugbot.mcp;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal MCP (Model Context Protocol) server over HTTP using JSON-RPC 2.0.
 *
 * <p>Supported methods:
 * <ul>
 *   <li>{@code initialize} – capability handshake</li>
 *   <li>{@code tools/list} – advertise the Jira tools</li>
 *   <li>{@code tools/call} – execute a named tool with arguments</li>
 * </ul>
 *
 * <p>An MCP-capable LLM client (Claude Desktop, VS Code MCP, a custom agent,
 * etc.) can point at {@code POST /mcp} and gain secure, typed access to Jira.
 */
@RestController
@RequestMapping(value = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
public class McpServerController {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final JiraMcpTools jiraTools;

    public McpServerController(JiraMcpTools jiraTools) {
        this.jiraTools = jiraTools;
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Map<String, Object> handle(@RequestBody Map<String, Object> req) {
        Object id = req.get("id");
        String method = String.valueOf(req.get("method"));
        Map<String, Object> params = req.get("params") instanceof Map
                ? (Map<String, Object>) req.get("params")
                : Map.of();

        try {
            return switch (method) {
                case "initialize" -> ok(id, initializeResult());
                case "tools/list" -> ok(id, Map.of("tools", toolDescriptors()));
                case "tools/call" -> ok(id, callTool(params));
                case "ping" -> ok(id, Map.of());
                default -> error(id, -32601, "Method not found: " + method);
            };
        } catch (IllegalArgumentException e) {
            return error(id, -32602, e.getMessage());
        } catch (Exception e) {
            return error(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    private Map<String, Object> initializeResult() {
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of("tools", Map.of("listChanged", false)),
                "serverInfo", Map.of("name", "bugbot-jira-mcp", "version", "1.0.0")
        );
    }

    private List<Map<String, Object>> toolDescriptors() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (McpTool t : jiraTools.tools()) {
            list.add(Map.of(
                    "name", t.name(),
                    "description", t.description(),
                    "inputSchema", t.inputSchema()
            ));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callTool(Map<String, Object> params) {
        String name = String.valueOf(params.get("name"));
        Map<String, Object> args = params.get("arguments") instanceof Map
                ? (Map<String, Object>) params.get("arguments")
                : new HashMap<>();

        McpTool tool = jiraTools.tools().stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));

        Object result = tool.handler().apply(args);

        // MCP expects tool results wrapped as "content" blocks.
        return Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", String.valueOf(result)
                )),
                "structuredContent", result,
                "isError", false
        );
    }

    // ---- JSON-RPC envelope helpers --------------------------------------

    private Map<String, Object> ok(Object id, Object result) {
        Map<String, Object> m = new HashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("result", result);
        return m;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("error", Map.of("code", code, "message", message));
        return m;
    }
}

