package com.utkarsh.tripmate.config;

import com.utkarsh.tripmate.service.interactive.ToolConfirmationAdvisor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicConnectionProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
public class AnthropicConfig {

    @Bean(name = "anthropicChatClient")
    public ChatClient anthropicChatClient(
            AnthropicConnectionProperties anthropicConnectionProperties,
            AnthropicChatProperties anthropicChatProperties,
            ObservationRegistry observationRegistry,
            @Lazy ToolCallingManager toolCallingManager,
            @Lazy ToolConfirmationAdvisor toolConfirmationAdvisor) {
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .apiKey(anthropicConnectionProperties.getApiKey())
                .build();
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
                .model(anthropicChatProperties.getOptions().getModel())
                .internalToolExecutionEnabled(false);
        if (anthropicChatProperties.getOptions().getTemperature() != null) {
            optionsBuilder.temperature(anthropicChatProperties.getOptions().getTemperature());
        }
        if (anthropicChatProperties.getOptions().getMaxTokens() != null) {
            optionsBuilder.maxTokens(anthropicChatProperties.getOptions().getMaxTokens());
        }
        AnthropicChatModel anthropicChatModel = new AnthropicChatModel(
                anthropicApi,
                optionsBuilder.build(),
                toolCallingManager,
                RetryUtils.DEFAULT_RETRY_TEMPLATE,
                observationRegistry);
        return ChatClient.builder(anthropicChatModel)
                .defaultAdvisors(List.of(toolConfirmationAdvisor))
                .build();
    }
}
