package org.example.bugbot.dashboard;

import java.time.Instant;
import java.util.List;

/**
 * The full payload the Teams Tab renders: the LLM "Daily Standup" bullet
 * summary at the top, plus tickets grouped by role.
 */
public record DashboardView(
        Instant generatedAt,
        int totalActive,
        List<String> standupBullets,  // the 3-bullet LLM summary
        List<RoleGroup> roleGroups
) {}

