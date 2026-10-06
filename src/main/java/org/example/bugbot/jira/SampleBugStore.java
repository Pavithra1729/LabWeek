package org.example.bugbot.jira;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sample bugs used when BugBot runs in <b>sample mode</b>
 * (i.e. no real Jira API token is configured). This lets you test the whole
 * pipeline — dashboard, standup summary, role grouping, VS Code deep links,
 * and ChatOps transitions/comments — with zero external setup.
 *
 * <p>The data is mutable: ChatOps {@code update}/{@code comment} commands
 * change the state here so you can see the effect on the dashboard.
 */
@Component
public class SampleBugStore {

    /** Issue key -> issue (preserves insertion order for stable output). */
    private final Map<String, JiraIssue> issues = new LinkedHashMap<>();
    /** Issue key -> comment list. */
    private final Map<String, List<String>> comments = new ConcurrentHashMap<>();

    public SampleBugStore() {
        seed();
    }

    private void seed() {
        // --- High-severity regressions blocking deployment (QA) ---
        add(new JiraIssue("QA-101",
                "Login page throws NPE after SSO redirect in src/pages/login.spec.ts:42",
                "Open", "To Do", "Highest", "Bug",
                "Priya Nair", "priya@demo.com",
                List.of("regression", "blocker", "qa"),
                now(), url("QA-101")),
                List.of("Repro 100% on staging after the 4.2 release.",
                        "Playwright trace attached — fails at the redirect step."));

        add(new JiraIssue("QA-102",
                "Checkout total mismatch for discounted carts (regression) src/checkout/total.ts:128",
                "In Progress", "In Progress", "High", "Bug",
                "Marco Rossi", "marco@demo.com",
                List.of("regression", "qa", "payments"),
                now(), url("QA-102")),
                List.of("Only reproduces when a coupon + gift card are combined."));

        add(new JiraIssue("QA-103",
                "Flaky E2E: cart badge count off-by-one src/components/cart-badge.spec.ts:17",
                "Open", "To Do", "Medium", "Bug",
                "Priya Nair", "priya@demo.com",
                List.of("qa", "test", "flaky"),
                now(), url("QA-103")),
                List.of());

        // --- Developer work waiting for code review / in progress ---
        add(new JiraIssue("DEV-201",
                "Add rate limiting to /api/messages endpoint src/api/messages.java:64",
                "In Review", "In Progress", "High", "Task",
                "Aisha Khan", "aisha@demo.com",
                List.of("backend", "security"),
                now(), url("DEV-201")),
                List.of("PR #482 open, waiting on a second reviewer."));

        add(new JiraIssue("DEV-202",
                "Refactor DashboardService role grouping src/dashboard/DashboardService.java:48",
                "In Review", "In Progress", "Medium", "Story",
                "Tom Becker", "tom@demo.com",
                List.of("backend", "refactor"),
                now(), url("DEV-202")),
                List.of());

        add(new JiraIssue("DEV-203",
                "Cache Jira search responses for 60s src/jira/JiraClient.java:35",
                "In Progress", "In Progress", "Low", "Task",
                "Aisha Khan", "aisha@demo.com",
                List.of("backend", "performance"),
                now(), url("DEV-203")),
                List.of());

        add(new JiraIssue("DEV-204",
                "Dark mode styles for Teams tab src/Dashboard.jsx:92",
                "Open", "To Do", "Low", "Story",
                "Tom Becker", "tom@demo.com",
                List.of("frontend", "ux"),
                now(), url("DEV-204")),
                List.of());
    }

    // ---- read ------------------------------------------------------------

    /** All non-Done issues, honouring a simple "key = X" filter if present. */
    public List<JiraIssue> search(String jql, int maxResults) {
        String key = extractKey(jql);
        List<JiraIssue> out = new ArrayList<>();
        for (JiraIssue i : issues.values()) {
            if (key != null && !i.key().equalsIgnoreCase(key)) continue;
            if (key == null && "Done".equalsIgnoreCase(i.statusCategory())) continue;
            out.add(i);
            if (out.size() >= maxResults) break;
        }
        return out;
    }

    public List<String> comments(String issueKey) {
        return comments.getOrDefault(issueKey.toUpperCase(), List.of());
    }

    // ---- mutate (ChatOps) ------------------------------------------------

    public void addComment(String issueKey, String text) {
        comments.computeIfAbsent(issueKey.toUpperCase(), k -> new ArrayList<>()).add(text);
    }

    /** Update status/statusCategory; returns false if the issue is unknown. */
    public boolean transition(String issueKey, String targetStatus) {
        JiraIssue cur = issues.get(issueKey.toUpperCase());
        if (cur == null) return false;
        issues.put(cur.key(), new JiraIssue(
                cur.key(), cur.summary(), targetStatus, categoryFor(targetStatus),
                cur.priority(), cur.issueType(), cur.assigneeName(), cur.assigneeEmail(),
                cur.labels(), now(), cur.url()));
        return true;
    }

    // ---- helpers ---------------------------------------------------------

    private void add(JiraIssue issue, List<String> issueComments) {
        issues.put(issue.key(), issue);
        comments.put(issue.key(), new ArrayList<>(issueComments));
    }

    private String categoryFor(String status) {
        String s = status.toLowerCase();
        if (s.contains("done") || s.contains("closed") || s.contains("resolved")) return "Done";
        if (s.contains("to do") || s.contains("open") || s.contains("backlog")) return "To Do";
        return "In Progress";
    }

    private String extractKey(String jql) {
        if (jql == null) return null;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("key\\s*=\\s*([A-Z]+-\\d+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(jql);
        return m.find() ? m.group(1).toUpperCase() : null;
    }

    private String now() {
        return Instant.now().toString();
    }

    private static String url(String key) {
        return "https://demo.atlassian.net/browse/" + key;
    }
}

