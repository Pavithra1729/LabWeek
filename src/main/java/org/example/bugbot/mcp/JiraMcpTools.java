package org.example.bugbot.mcp;

import org.example.bugbot.jira.JiraClient;
import org.example.bugbot.jira.JiraIssue;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Registers the Jira tools that the LLM is allowed to call through MCP.
 *
 * <p>Each tool maps to a capability on {@link JiraClient}. Keeping this
 * registry small and explicit is what makes the integration "secure":
 * the model can only do what you hand it here.
 */
@Component
public class JiraMcpTools {

    private final JiraClient jira;

    public JiraMcpTools(JiraClient jira) {
        this.jira = jira;
    }

    public List<McpTool> tools() {
        return List.of(
                searchTool(),
                commentsTool(),
                addCommentTool(),
                transitionTool()
        );
    }

    private McpTool searchTool() {
        return new McpTool(
                "jira.search",
                "Search Jira issues using JQL. Returns a list of normalised issues.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "jql", Map.of("type", "string",
                                        "description", "A valid JQL query string."),
                                "maxResults", Map.of("type", "integer", "default", 50)
                        ),
                        "required", List.of("jql")
                ),
                args -> {
                    String jql = String.valueOf(args.get("jql"));
                    int max = args.get("maxResults") == null ? 50
                            : ((Number) args.get("maxResults")).intValue();
                    List<JiraIssue> issues = jira.search(jql, max);
                    return Map.of("count", issues.size(), "issues", issues);
                }
        );
    }

    private McpTool commentsTool() {
        return new McpTool(
                "jira.getComments",
                "Get the plain-text comments for a single Jira issue.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "issueKey", Map.of("type", "string",
                                        "description", "e.g. QA-123")
                        ),
                        "required", List.of("issueKey")
                ),
                args -> Map.of("comments", jira.comments(String.valueOf(args.get("issueKey"))))
        );
    }

    private McpTool addCommentTool() {
        return new McpTool(
                "jira.addComment",
                "Add a comment to a Jira issue.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "issueKey", Map.of("type", "string"),
                                "text", Map.of("type", "string")
                        ),
                        "required", List.of("issueKey", "text")
                ),
                args -> {
                    jira.addComment(String.valueOf(args.get("issueKey")),
                            String.valueOf(args.get("text")));
                    return Map.of("ok", true);
                }
        );
    }

    private McpTool transitionTool() {
        return new McpTool(
                "jira.transition",
                "Move a Jira issue to a new status, e.g. 'In Review' or 'Done'.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "issueKey", Map.of("type", "string"),
                                "status", Map.of("type", "string",
                                        "description", "Target status name.")
                        ),
                        "required", List.of("issueKey", "status")
                ),
                args -> {
                    boolean ok = jira.transition(
                            String.valueOf(args.get("issueKey")),
                            String.valueOf(args.get("status")));
                    return Map.of("ok", ok);
                }
        );
    }
}

