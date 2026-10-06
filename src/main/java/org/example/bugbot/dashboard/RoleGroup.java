package org.example.bugbot.dashboard;

import java.util.List;

/** Tickets grouped under a role, e.g. "QA / Tester" or "Developer". */
public record RoleGroup(
        String role,
        int total,
        int highSeverity,
        List<TicketView> tickets
) {}

