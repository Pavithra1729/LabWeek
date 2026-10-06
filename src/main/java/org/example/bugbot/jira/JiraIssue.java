package org.example.bugbot.jira;

import java.util.List;

/**
 * A normalised, flattened view of a Jira issue that is easy for both the
 * LLM and the Teams dashboard to consume. We deliberately avoid exposing
 * Jira's deeply-nested raw JSON shape to the rest of the app.
 */
public record JiraIssue(
        String key,            // e.g. "QA-123"
        String summary,
        String status,         // e.g. "In Review"
        String statusCategory, // e.g. "In Progress" / "Done" / "To Do"
        String priority,       // e.g. "Highest", "High", ...
        String issueType,      // e.g. "Bug", "Task", "Story"
        String assigneeName,
        String assigneeEmail,
        List<String> labels,
        String updated,
        String url             // browse URL into Jira
) {
    /** High-severity = Highest/High priority OR a Bug/regression. */
    public boolean isHighSeverity() {
        boolean highPriority = "Highest".equalsIgnoreCase(priority)
                || "High".equalsIgnoreCase(priority);
        boolean regression = labels != null && labels.stream()
                .anyMatch(l -> l.toLowerCase().contains("regression"));
        return highPriority || regression;
    }

    public boolean isWaitingForReview() {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.contains("review") || s.contains("code review");
    }

    public boolean isBlocker() {
        if (labels == null) return false;
        return labels.stream().anyMatch(l ->
                l.toLowerCase().contains("blocker") || l.toLowerCase().contains("blocking"));
    }
}

