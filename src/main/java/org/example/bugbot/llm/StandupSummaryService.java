package org.example.bugbot.llm;

import org.example.bugbot.jira.JiraIssue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The LLM pipeline. Turns the raw active Jira tickets into a concise,
 * action-oriented 3-bullet "Daily Standup" summary for the top of the
 * Teams dashboard.
 *
 * <p>If the LLM is disabled/unavailable it degrades gracefully to a
 * deterministic, metrics-based summary so the dashboard always renders.
 */
@Service
public class StandupSummaryService {

    private static final String SYSTEM_PROMPT = """
            You are BugBot, an engineering manager's assistant.
            You will be given a list of active Jira tickets for a software team.
            Produce EXACTLY 3 short bullet points for a daily standup.
            Rules:
              - Each bullet is ONE sentence, action-oriented and specific.
              - Call out high-severity regressions blocking deployment first.
              - Group insight by role (QA/Testers vs Developers) where useful.
              - Mention counts (e.g. "5 tickets waiting for code review").
              - Return ONLY the 3 bullets, each starting with "- ". No preamble.
            """;

    private final LlmClient llm;

    public StandupSummaryService(LlmClient llm) {
        this.llm = llm;
    }

    public List<String> summarise(List<JiraIssue> issues) {
        if (issues.isEmpty()) {
            return List.of("- No active tickets. Enjoy the quiet! 🎉");
        }
        if (!llm.isEnabled()) {
            return fallbackSummary(issues);
        }
        try {
            String content = llm.complete(SYSTEM_PROMPT, renderTicketsForPrompt(issues));
            List<String> bullets = Arrays.stream(content.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(l -> l.startsWith("-") || l.startsWith("•"))
                    .map(l -> l.replaceFirst("^[-•]\\s*", "- "))
                    .limit(3)
                    .collect(Collectors.toList());
            return bullets.isEmpty() ? fallbackSummary(issues) : bullets;
        } catch (Exception e) {
            // Never let the dashboard fail because the LLM is down.
            return fallbackSummary(issues);
        }
    }

    /** Compact textual representation of tickets fed to the model. */
    private String renderTicketsForPrompt(List<JiraIssue> issues) {
        StringBuilder sb = new StringBuilder("Active tickets:\n");
        for (JiraIssue i : issues) {
            sb.append("- [").append(i.key()).append("] ")
              .append(i.issueType()).append(" | ")
              .append("priority=").append(i.priority()).append(" | ")
              .append("status=").append(i.status()).append(" | ")
              .append("assignee=").append(i.assigneeName()).append(" | ")
              .append("labels=").append(i.labels()).append(" | ")
              .append(i.summary()).append('\n');
        }
        return sb.toString();
    }

    /** Deterministic summary when no LLM is configured. */
    private List<String> fallbackSummary(List<JiraIssue> issues) {
        long highSev = issues.stream().filter(JiraIssue::isHighSeverity).count();
        long inReview = issues.stream().filter(JiraIssue::isWaitingForReview).count();
        long bugs = issues.stream()
                .filter(i -> "Bug".equalsIgnoreCase(i.issueType())).count();

        List<String> bullets = new ArrayList<>();
        bullets.add(String.format(
                "- QA has %d high-severity item(s) that may block deployment.", highSev));
        bullets.add(String.format(
                "- Developers have %d ticket(s) waiting for code review.", inReview));
        bullets.add(String.format(
                "- %d active bug(s) across %d total open tickets.", bugs, issues.size()));
        return bullets;
    }
}

