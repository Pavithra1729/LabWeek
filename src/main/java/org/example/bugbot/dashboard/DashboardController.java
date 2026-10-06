package org.example.bugbot.dashboard;

import org.example.bugbot.teams.AdaptiveCardBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API consumed by the Microsoft Teams Tab (React frontend).
 *
 * <ul>
 *   <li>{@code GET /api/dashboard} – structured JSON for the React tab</li>
 *   <li>{@code GET /api/dashboard/card} – Adaptive Card JSON for bots/cards</li>
 * </ul>
 *
 * CORS is opened for the Teams tab host; lock this down to your tenant in prod.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"https://teams.microsoft.com", "http://localhost:53000"})
public class DashboardController {

    private final DashboardService dashboardService;
    private final AdaptiveCardBuilder cardBuilder;

    public DashboardController(DashboardService dashboardService,
                              AdaptiveCardBuilder cardBuilder) {
        this.dashboardService = dashboardService;
        this.cardBuilder = cardBuilder;
    }

    @GetMapping
    public DashboardView dashboard() {
        return dashboardService.build();
    }

    @GetMapping("/card")
    public Map<String, Object> card() {
        return cardBuilder.build(dashboardService.build());
    }
}

