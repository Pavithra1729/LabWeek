package org.example.bugbot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.bugbot.config.LlmProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Tiny wrapper around an OpenAI-compatible chat-completions API.
 * Keeps the rest of the app decoupled from the specific provider.
 */
@Service
public class LlmClient {

    private final WebClient llm;
    private final LlmProperties props;

    public LlmClient(@Qualifier("llmWebClient") WebClient llm, LlmProperties props) {
        this.llm = llm;
        this.props = props;
    }

    public boolean isEnabled() {
        return props.enabled() && props.apiKey() != null && !props.apiKey().isBlank();
    }

    /**
     * Single-turn completion.
     *
     * @param system system prompt (role + formatting rules)
     * @param user   user prompt (the data to summarise)
     * @return the assistant message content
     */
    public String complete(String system, String user) {
        JsonNode resp = llm.post()
                .uri("/chat/completions")
                .bodyValue(Map.of(
                        "model", props.model() == null ? "gpt-4o-mini" : props.model(),
                        "temperature", 0.2,
                        "messages", List.of(
                                Map.of("role", "system", "content", system),
                                Map.of("role", "user", "content", user)
                        )
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (resp == null) return "";
        return resp.path("choices").path(0).path("message").path("content").asText("");
    }
}

