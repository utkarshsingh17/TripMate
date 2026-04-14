package com.utkarsh.tripmate.config;

import com.utkarsh.tripmate.service.interactive.ToolConfirmationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DeepSeekConfig {

    @Bean(name = "deepSeekChatClient")
    public ChatClient deepSeekChatClient(
            DeepSeekChatModel deepSeekChatModel,
            ToolConfirmationAdvisor toolConfirmationAdvisor) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(List.of(toolConfirmationAdvisor))
                .build();
    }
}
