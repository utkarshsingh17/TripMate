package com.utkarsh.tripmate.config;

import com.utkarsh.tripmate.service.interactive.ToolConfirmationAdvisor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatProperties;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekConnectionProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
public class DeepSeekConfig {

    @Bean(name = "deepSeekChatClient")
    public ChatClient deepSeekChatClient(
            DeepSeekConnectionProperties deepSeekConnectionProperties,
            DeepSeekChatProperties deepSeekChatProperties,
            ObservationRegistry observationRegistry,
            @Lazy ToolCallingManager toolCallingManager,
            @Lazy ToolConfirmationAdvisor toolConfirmationAdvisor) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(deepSeekConnectionProperties.getApiKey())
                .build();
        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder()
                .model(deepSeekChatProperties.getOptions().getModel())
                .internalToolExecutionEnabled(false);
        if (deepSeekChatProperties.getOptions().getTemperature() != null) {
            optionsBuilder.temperature(deepSeekChatProperties.getOptions().getTemperature());
        }
        if (deepSeekChatProperties.getOptions().getMaxTokens() != null) {
            optionsBuilder.maxTokens(deepSeekChatProperties.getOptions().getMaxTokens());
        }
        DeepSeekChatModel deepSeekChatModel = new DeepSeekChatModel(
                deepSeekApi,
                optionsBuilder.build(),
                toolCallingManager,
                RetryUtils.DEFAULT_RETRY_TEMPLATE,
                observationRegistry);
        return ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(List.of(toolConfirmationAdvisor))
                .build();
    }
}
