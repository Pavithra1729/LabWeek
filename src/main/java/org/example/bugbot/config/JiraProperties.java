package org.example.bugbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jira connection settings. Bind from {@code bugbot.jira.*} in application.yml
 * (or environment variables, e.g. BUGBOT_JIRA_TOKEN).
 *
 * @param baseUrl   e.g. https://your-org.atlassian.net
 * @param email     the account email used for Basic auth (Atlassian Cloud)
 * @param apiToken  an Atlassian API token (never commit this!)
 * @param projectKey default project to query, e.g. "QA"
 * @param boardJql  base JQL filter for "active" tickets the team cares about
 */
@ConfigurationProperties(prefix = "bugbot.jira")
public record JiraProperties(
        String baseUrl,
        String email,
        String apiToken,
        String projectKey,
        String boardJql
) {
    public String boardJqlOrDefault() {
        if (boardJql != null && !boardJql.isBlank()) {
            return boardJql;
        }
        // Everything open/in-progress that is not Done, newest first.
        return "statusCategory != Done ORDER BY priority DESC, updated DESC";
    }

    /**
     * Sample mode is ON when no real API token is configured. In this mode
     * BugBot serves in-memory sample bugs so the whole system can be tested
     * without a live Jira instance.
     */
    public boolean sampleMode() {
        return apiToken == null || apiToken.isBlank();
    }
}

