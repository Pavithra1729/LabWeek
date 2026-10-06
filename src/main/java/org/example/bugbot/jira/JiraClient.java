package org.example.bugbot.jira;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.bugbot.config.JiraProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin client over the Jira Cloud REST API (v3). It knows how to:
 * <ul>
 *   <li>search issues via JQL</li>
 *   <li>read comments</li>
 *   <li>transition a ticket's status</li>
 *   <li>add a comment</li>
 * </ul>
 * These capabilities are what the {@code JiraMcpTools} exposes to the LLM.
 */
@Service
public class JiraClient {

    private final WebClient jira;
    private final JiraProperties props;
    private final SampleBugStore sampleStore;

    public JiraClient(@Qualifier("jiraWebClient") WebClient jira,
                      JiraProperties props,
                      SampleBugStore sampleStore) {
        this.jira = jira;
        this.props = props;
        this.sampleStore = sampleStore;
    }

    /** Search issues using JQL and map them to {@link JiraIssue}. */
    public List<JiraIssue> search(String jql, int maxResults) {
        if (props.sampleMode()) {
            return sampleStore.search(jql, maxResults);
        }
        JsonNode root = jira.post()
                .uri("/rest/api/3/search")
                .bodyValue(Map.of(
                        "jql", jql,
                        "maxResults", maxResults,
                        "fields", List.of("summary", "status", "priority",
                                "issuetype", "assignee", "labels", "updated")
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<JiraIssue> issues = new ArrayList<>();
        if (root == null || !root.has("issues")) {
            return issues;
        }
        for (JsonNode node : root.get("issues")) {
            issues.add(mapIssue(node));
        }
        return issues;
    }

    /** Convenience: the team's active board, using the configured base JQL. */
    public List<JiraIssue> activeBoardIssues(int maxResults) {
        String projectFilter = props.projectKey() == null || props.projectKey().isBlank()
                ? ""
                : "project = " + props.projectKey() + " AND ";
        return search(projectFilter + props.boardJqlOrDefault(), maxResults);
    }

    /** Fetch comment bodies for a ticket (plain text, newest last). */
    public List<String> comments(String issueKey) {
        if (props.sampleMode()) {
            return sampleStore.comments(issueKey);
        }
        JsonNode root = jira.get()
                .uri("/rest/api/3/issue/{key}/comment", issueKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<String> out = new ArrayList<>();
        if (root == null || !root.has("comments")) return out;
        for (JsonNode c : root.get("comments")) {
            out.add(plainText(c.path("body")));
        }
        return out;
    }

    /** Add a comment to an issue (used by ChatOps). */
    public void addComment(String issueKey, String text) {
        if (props.sampleMode()) {
            sampleStore.addComment(issueKey, text);
            return;
        }
        jira.post()
                .uri("/rest/api/3/issue/{key}/comment", issueKey)
                .bodyValue(Map.of("body", adfParagraph(text)))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    /**
     * Transition an issue to a new status by matching the transition name
     * (case-insensitive), e.g. "In Review", "Done".
     *
     * @return true if a matching transition was found and applied.
     */
    public boolean transition(String issueKey, String targetStatusName) {
        if (props.sampleMode()) {
            return sampleStore.transition(issueKey, targetStatusName);
        }
        JsonNode root = jira.get()
                .uri("/rest/api/3/issue/{key}/transitions", issueKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null || !root.has("transitions")) return false;

        String transitionId = null;
        for (JsonNode t : root.get("transitions")) {
            String name = t.path("to").path("name").asText();
            if (name.equalsIgnoreCase(targetStatusName)
                    || t.path("name").asText().equalsIgnoreCase(targetStatusName)) {
                transitionId = t.path("id").asText();
                break;
            }
        }
        if (transitionId == null) return false;

        jira.post()
                .uri("/rest/api/3/issue/{key}/transitions", issueKey)
                .bodyValue(Map.of("transition", Map.of("id", transitionId)))
                .retrieve()
                .toBodilessEntity()
                .block();
        return true;
    }

    // ---------------------------------------------------------------------
    // mapping helpers
    // ---------------------------------------------------------------------

    private JiraIssue mapIssue(JsonNode node) {
        String key = node.path("key").asText();
        JsonNode f = node.path("fields");

        List<String> labels = new ArrayList<>();
        if (f.has("labels")) {
            f.get("labels").forEach(l -> labels.add(l.asText()));
        }

        return new JiraIssue(
                key,
                f.path("summary").asText(""),
                f.path("status").path("name").asText(""),
                f.path("status").path("statusCategory").path("name").asText(""),
                f.path("priority").path("name").asText("None"),
                f.path("issuetype").path("name").asText(""),
                f.path("assignee").path("displayName").asText("Unassigned"),
                f.path("assignee").path("emailAddress").asText(""),
                labels,
                f.path("updated").asText(""),
                props.baseUrl() + "/browse/" + key
        );
    }

    /** Flatten Atlassian Document Format (ADF) comment body into plain text. */
    private String plainText(JsonNode adf) {
        if (adf == null || adf.isMissingNode()) return "";
        if (adf.isTextual()) return adf.asText();
        StringBuilder sb = new StringBuilder();
        if (adf.has("text")) sb.append(adf.get("text").asText());
        if (adf.has("content")) {
            for (JsonNode child : adf.get("content")) {
                sb.append(plainText(child)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /** Build a minimal ADF document from plain text for comment creation. */
    private Map<String, Object> adfParagraph(String text) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", text))
                ))
        );
    }
}

