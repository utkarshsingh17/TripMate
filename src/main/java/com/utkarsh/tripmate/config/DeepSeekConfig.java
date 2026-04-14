package com.utkarsh.tripmate.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {

    @Bean(name = "deepSeekChatClient")
    public ChatClient deepSeekChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel).build();
    }
}
