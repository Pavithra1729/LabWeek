package org.example.bugbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM provider settings. Works with OpenAI or Azure OpenAI compatible
 * chat-completions endpoints. Bind from {@code bugbot.llm.*}.
 *
 * @param baseUrl  e.g. https://api.openai.com/v1  (or your Azure endpoint)
 * @param apiKey   provider API key (keep in env var BUGBOT_LLM_APIKEY)
 * @param model    e.g. gpt-4o-mini
 * @param enabled  if false, BugBot falls back to a deterministic template summary
 */
@ConfigurationProperties(prefix = "bugbot.llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String model,
        boolean enabled
) {}

