package com.utkarsh.tripmate.config;

import com.utkarsh.tripmate.service.interactive.ToolConfirmationAdvisor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AnthropicConfig {

    @Bean(name = "anthropicChatClient")
    public ChatClient anthropicChatClient(
            AnthropicChatModel anthropicChatModel,
            ToolConfirmationAdvisor toolConfirmationAdvisor) {
        return ChatClient.builder(anthropicChatModel)
                .defaultAdvisors(List.of(toolConfirmationAdvisor))
                .build();
    }
}
