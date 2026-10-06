package org.example.bugbot.dashboard;

import org.example.bugbot.jira.JiraClient;
import org.example.bugbot.jira.JiraIssue;
import org.example.bugbot.llm.StandupSummaryService;
import org.example.bugbot.teams.DeepLinkService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assembles the full dashboard: pulls active tickets via Jira, asks the LLM
 * for the standup summary, groups tickets by role, and attaches VS Code
 * deep links.
 */
@Service
public class DashboardService {

    private final JiraClient jira;
    private final StandupSummaryService standup;
    private final DeepLinkService deepLinks;

    public DashboardService(JiraClient jira,
                            StandupSummaryService standup,
                            DeepLinkService deepLinks) {
        this.jira = jira;
        this.standup = standup;
        this.deepLinks = deepLinks;
    }

    public DashboardView build() {
        List<JiraIssue> issues = jira.activeBoardIssues(50);

        List<String> bullets = standup.summarise(issues);

        // Group by role. In Jira this is usually derived from issue type,
        // a component, or a label. Here we infer: Bugs/regressions => QA,
        // everything else => Developer. Adapt to your workflow.
        Map<String, List<JiraIssue>> byRole = issues.stream()
                .collect(Collectors.groupingBy(this::roleOf));

        List<RoleGroup> groups = byRole.entrySet().stream()
                .map(e -> toRoleGroup(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.highSeverity(), a.highSeverity()))
                .toList();

        return new DashboardView(Instant.now(), issues.size(), bullets, groups);
    }

    private String roleOf(JiraIssue issue) {
        String type = issue.issueType() == null ? "" : issue.issueType().toLowerCase();
        boolean qa = type.contains("bug")
                || (issue.labels() != null && issue.labels().stream()
                        .anyMatch(l -> l.toLowerCase().contains("qa")
                                || l.toLowerCase().contains("regression")
                                || l.toLowerCase().contains("test")));
        return qa ? "QA / Tester" : "Developer";
    }

    private RoleGroup toRoleGroup(String role, List<JiraIssue> issues) {
        List<TicketView> tickets = issues.stream()
                .map(this::toTicketView)
                .sorted((a, b) -> Boolean.compare(b.highSeverity(), a.highSeverity()))
                .toList();
        int highSev = (int) issues.stream().filter(JiraIssue::isHighSeverity).count();
        return new RoleGroup(role, issues.size(), highSev, tickets);
    }

    private TicketView toTicketView(JiraIssue i) {
        return new TicketView(
                i.key(),
                i.summary(),
                i.status(),
                i.priority(),
                i.issueType(),
                i.assigneeName(),
                i.isHighSeverity(),
                i.url(),
                deepLinks.forIssue(i),
                i.labels()
        );
    }
}

