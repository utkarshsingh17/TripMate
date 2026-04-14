package com.utkarsh.tripmate.config;

import com.utkarsh.tripmate.service.interactive.ToolConfirmationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAiConfig {

    @Bean(name = "openAiChatClient")
    public ChatClient openAiChatClient(
            OpenAiChatModel openAiChatModel,
            ToolConfirmationAdvisor toolConfirmationAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(List.of(toolConfirmationAdvisor))
                .build();
    }
}
