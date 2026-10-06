package org.example.bugbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * BugBot entry point.
 *
 * <p>Architecture (see README in this package):
 * <pre>
 *   Microsoft Teams Tab (React)  ─┐
 *                                 ├──▶  BugBot Spring Boot backend
 *   Teams Bot (@BugBot ChatOps) ─┘          │
 *                                            ├─▶ Jira MCP server (JSON-RPC tools)
 *                                            ├─▶ LLM pipeline (standup summary)
 *                                            └─▶ Adaptive Cards + VS Code deep links
 * </pre>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BugBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BugBotApplication.class, args);
    }
}

