package org.example.bugbot.dashboard;

import java.util.List;

/**
 * A single ticket as presented on the Teams dashboard, including the
 * contextual VS Code deep link ("jump to code") built by DeepLinkService.
 */
public record TicketView(
        String key,
        String summary,
        String status,
        String priority,
        String issueType,
        String assignee,
        boolean highSeverity,
        String jiraUrl,
        String vsCodeDeepLink,   // vscode://... jump link
        List<String> labels
) {}

