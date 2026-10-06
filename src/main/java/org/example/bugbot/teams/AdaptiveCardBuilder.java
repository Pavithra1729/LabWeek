package org.example.bugbot.teams;

import org.example.bugbot.dashboard.DashboardView;
import org.example.bugbot.dashboard.RoleGroup;
import org.example.bugbot.dashboard.TicketView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a {@link DashboardView} as a Microsoft Teams
 * <a href="https://adaptivecards.io">Adaptive Card</a> JSON payload.
 *
 * <p>This is what the Teams bot posts into a channel, and is an alternative
 * to the React tab for teams that prefer a card-in-chat experience.
 */
@Service
public class AdaptiveCardBuilder {

    public Map<String, Object> build(DashboardView view) {
        List<Object> body = new ArrayList<>();

        body.add(Map.of(
                "type", "TextBlock",
                "text", "🐞 BugBot — Daily Standup",
                "weight", "Bolder",
                "size", "Large"
        ));
        body.add(Map.of(
                "type", "TextBlock",
                "text", view.totalActive() + " active tickets",
                "isSubtle", true,
                "spacing", "None"
        ));

        // The LLM 3-bullet summary.
        for (String bullet : view.standupBullets()) {
            body.add(Map.of(
                    "type", "TextBlock",
                    "text", bullet,
                    "wrap", true
            ));
        }

        // One section per role.
        for (RoleGroup group : view.roleGroups()) {
            body.add(Map.of(
                    "type", "TextBlock",
                    "text", group.role() + "  (" + group.total() + ", "
                            + group.highSeverity() + " high-sev)",
                    "weight", "Bolder",
                    "spacing", "Medium",
                    "separator", true
            ));
            for (TicketView t : group.tickets()) {
                body.add(ticketRow(t));
            }
        }

        return Map.of(
                "type", "AdaptiveCard",
                "$schema", "http://adaptivecards.io/schemas/adaptive-card.json",
                "version", "1.5",
                "body", body
        );
    }

    private Map<String, Object> ticketRow(TicketView t) {
        String severity = t.highSeverity() ? "🔴 " : "⚪ ";
        return Map.of(
                "type", "ColumnSet",
                "columns", List.of(
                        Map.of("type", "Column", "width", "stretch", "items", List.of(
                                Map.of("type", "TextBlock",
                                        "text", severity + "**" + t.key() + "** " + t.summary(),
                                        "wrap", true),
                                Map.of("type", "TextBlock",
                                        "text", t.status() + " · " + t.priority()
                                                + " · " + t.assignee(),
                                        "isSubtle", true, "spacing", "None", "size", "Small")
                        )),
                        Map.of("type", "Column", "width", "auto", "items", List.of(
                                Map.of("type", "ActionSet", "actions", List.of(
                                        Map.of("type", "Action.OpenUrl",
                                                "title", "Jira", "url", t.jiraUrl()),
                                        Map.of("type", "Action.OpenUrl",
                                                "title", "Open in VS Code",
                                                "url", t.vsCodeDeepLink())
                                ))
                        ))
                )
        );
    }
}

