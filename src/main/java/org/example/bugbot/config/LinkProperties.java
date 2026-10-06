package org.example.bugbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings used to build "contextual jump links" into the developer's
 * VS Code workspace (deep links via the vscode:// custom protocol).
 *
 * @param repoRootUri  base URI VS Code understands, e.g.
 *                     {@code vscode://file//Users/you/project} or a
 *                     {@code vscode://vscode.git/clone?url=...} style link.
 * @param teamsAppId   the Teams app/tab id, used to build deep links back
 *                     into the Teams tab for a given ticket.
 */
@ConfigurationProperties(prefix = "bugbot.links")
public record LinkProperties(
        String repoRootUri,
        String teamsAppId
) {}

