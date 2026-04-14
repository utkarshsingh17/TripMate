package com.utkarsh.tripmate.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    @Bean(name = "anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel).build();
    }
}
