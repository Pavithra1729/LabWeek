package org.example.bugbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Pre-configured {@link WebClient} beans for Jira and the LLM provider.
 */
@Configuration
public class WebClientConfig {

    /** WebClient pointing at Jira Cloud with Basic auth pre-applied. */
    @Bean("jiraWebClient")
    public WebClient jiraWebClient(JiraProperties props) {
        String raw = props.email() + ":" + props.apiToken();
        String basic = Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Basic " + basic)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /** WebClient pointing at the LLM provider with Bearer auth. */
    @Bean("llmWebClient")
    public WebClient llmWebClient(LlmProperties props) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(props.baseUrl() == null ? "https://api.openai.com/v1" : props.baseUrl())
                .defaultHeader("Content-Type", "application/json");
        if (props.apiKey() != null && !props.apiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.apiKey());
        }
        return builder.build();
    }
}

