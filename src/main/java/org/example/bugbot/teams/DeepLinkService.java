package org.example.bugbot.teams;

import org.example.bugbot.config.LinkProperties;
import org.example.bugbot.jira.JiraIssue;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds "contextual jump links" that open the relevant code or Playwright
 * trace directly in the developer's VS Code workspace.
 *
 * <p>Two strategies are supported:
 * <ol>
 *   <li><b>Direct file+line</b> – if a Jira ticket mentions a file path and
 *       line (e.g. {@code src/app/login.ts:42}), we build a
 *       {@code vscode://file/<abs-path>:line:col} link.</li>
 *   <li><b>Custom protocol handler</b> – fall back to a custom
 *       {@code vscode://your.extension/open?ticket=KEY} link that your VS Code
 *       extension interprets (e.g. to open the related Playwright trace).</li>
 * </ol>
 */
@Service
public class DeepLinkService {

    // Matches things like "src/pages/login.spec.ts:42" inside the summary/labels.
    private static final Pattern FILE_LINE =
            Pattern.compile("([\\w./\\-]+\\.(?:ts|tsx|js|jsx|java|py|spec\\.ts)):(\\d+)");

    private final LinkProperties props;

    public DeepLinkService(LinkProperties props) {
        this.props = props;
    }

    /**
     * Produce the best deep link we can for an issue.
     */
    public String forIssue(JiraIssue issue) {
        String haystack = issue.summary() + " "
                + (issue.labels() == null ? "" : String.join(" ", issue.labels()));

        Matcher m = FILE_LINE.matcher(haystack);
        if (m.find() && props.repoRootUri() != null) {
            String relPath = m.group(1);
            String line = m.group(2);
            // vscode://file/<absolute-path>:<line>:<col>
            String root = props.repoRootUri().replaceFirst("/+$", "");
            return root + "/" + relPath + ":" + line + ":1";
        }

        // Fallback: let a custom VS Code extension resolve the ticket.
        String ticket = URLEncoder.encode(issue.key(), StandardCharsets.UTF_8);
        return "vscode://bugbot.jira-linker/open?ticket=" + ticket;
    }

    /** Deep link back into the Teams tab focused on a specific ticket. */
    public String teamsTabLink(String issueKey) {
        if (props.teamsAppId() == null) return "";
        String ctx = URLEncoder.encode("{\"subEntityId\":\"" + issueKey + "\"}",
                StandardCharsets.UTF_8);
        return "https://teams.microsoft.com/l/entity/" + props.teamsAppId()
                + "/bugbot-dashboard?context=" + ctx;
    }
}

