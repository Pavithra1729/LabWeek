package org.example.bugbot.teams;

import org.example.bugbot.jira.JiraClient;
import org.example.bugbot.jira.JiraIssue;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprets Interactive ChatOps commands typed at the bot in Teams, e.g.
 * <pre>
 *   @BugBot status JIRA-123
 *   @BugBot update JIRA-123 In Review
 *   @BugBot comment JIRA-123 Looks good, merging now
 *   @BugBot summary
 * </pre>
 *
 * <p>This deterministic parser is the safe default. For a truly natural
 * language experience, route the raw text to the LLM and let it call the
 * MCP tools (see README "Agentic ChatOps").
 */
@Service
public class ChatOpsService {

    private static final Pattern JIRA_KEY = Pattern.compile("([A-Z][A-Z0-9]+-\\d+)");

    private final JiraClient jira;

    public ChatOpsService(JiraClient jira) {
        this.jira = jira;
    }

    public String handle(String rawText) {
        String text = stripMention(rawText).trim();
        String lower = text.toLowerCase();

        if (lower.startsWith("summary") || lower.startsWith("standup")) {
            return "Posting the latest standup card… (use the dashboard card endpoint).";
        }

        Matcher km = JIRA_KEY.matcher(text);
        if (lower.startsWith("status")) {
            if (!km.find()) return "Usage: `@BugBot status JIRA-123`";
            return statusOf(km.group(1));
        }
        if (lower.startsWith("update") || lower.startsWith("move") || lower.startsWith("transition")) {
            if (!km.find()) return "Usage: `@BugBot update JIRA-123 In Review`";
            String key = km.group(1);
            String target = text.substring(km.end()).trim();
            if (target.isBlank()) return "Tell me the target status, e.g. `In Review`.";
            boolean ok = jira.transition(key, target);
            return ok
                    ? "✅ Moved **" + key + "** to *" + target + "*."
                    : "⚠️ Couldn't find a transition to *" + target + "* for " + key + ".";
        }
        if (lower.startsWith("comment")) {
            if (!km.find()) return "Usage: `@BugBot comment JIRA-123 your note`";
            String key = km.group(1);
            String note = text.substring(km.end()).trim();
            if (note.isBlank()) return "What should I comment?";
            jira.addComment(key, note + "\n\n— via BugBot");
            return "💬 Comment added to **" + key + "**.";
        }

        return """
               I understand:
               • `@BugBot status JIRA-123`
               • `@BugBot update JIRA-123 In Review`
               • `@BugBot comment JIRA-123 <text>`
               • `@BugBot summary`
               """;
    }

    private String statusOf(String key) {
        List<JiraIssue> found = jira.search("key = " + key, 1);
        if (found.isEmpty()) return "Couldn't find " + key + ".";
        JiraIssue i = found.get(0);
        return String.format("**%s** — %s%n*%s* · %s · assigned to %s",
                i.key(), i.summary(), i.status(), i.priority(), i.assigneeName());
    }

    private String stripMention(String text) {
        if (text == null) return "";
        // Teams sends mentions as "<at>BugBot</at> ..." or "@BugBot ...".
        return text.replaceAll("(?i)<at>.*?</at>", "")
                   .replaceAll("(?i)@bugbot", "")
                   .trim();
    }
}

