package com.utkarsh.tripmate.config;

import com.utkarsh.tripmate.config.properties.McpRetryProperties;
import com.utkarsh.tripmate.service.AirfareService;
import com.utkarsh.tripmate.service.BookingService;
import com.utkarsh.tripmate.service.CurrencyExchangeService;
import com.utkarsh.tripmate.service.FinancialService;
import com.utkarsh.tripmate.service.RecipeService;
import com.utkarsh.tripmate.service.SpendingLogsService;
import com.utkarsh.tripmate.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FunctionCallingConfig {

    private static final Logger log = LoggerFactory.getLogger(FunctionCallingConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public List<ToolCallback> allAvailableToolCallbacks(
            WeatherService weatherService,
            AirfareService airfareService,
            CurrencyExchangeService currencyExchangeService,
            FinancialService financialService,
            RecipeService recipeService,
            BookingService bookingService,
            SpendingLogsService spendingLogsService,
            ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider,
            McpRetryProperties mcpRetryProperties) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.addAll(List.of(ToolCallbacks.from(
                weatherService,
                airfareService,
                currencyExchangeService,
                financialService,
                recipeService,
                bookingService,
                spendingLogsService
        )));

        SyncMcpToolCallbackProvider provider;
        try {
            provider = mcpToolCallbackProvider.getIfAvailable();
        } catch (Exception ex) {
            log.warn("MCP callback provider is not available at startup: {}", ex.getMessage());
            provider = null;
        }
        if (provider != null) {
            ToolCallback[] mcpCallbacks = fetchMcpCallbacksWithRetry(provider, mcpRetryProperties);
            if (mcpCallbacks != null && mcpCallbacks.length > 0) {
                toolCallbacks.addAll(Arrays.stream(mcpCallbacks).toList());
            }
        }

        log.info("Registered {} total tool callbacks", toolCallbacks.size());
        return toolCallbacks;
    }

    private ToolCallback[] fetchMcpCallbacksWithRetry(
            SyncMcpToolCallbackProvider provider,
            McpRetryProperties retryProperties) {
        int attempts = Math.max(1, retryProperties.getMaxAttempts());
        long backoffMillis = Math.max(0, retryProperties.getBackoffMillis());
        Exception lastException = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return provider.getToolCallbacks();
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Failed to load MCP callbacks (attempt {}/{}): {}", attempt, attempts, ex.getMessage());
                if (attempt < attempts && backoffMillis > 0) {
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while retrying MCP callback registration", interruptedException);
                    }
                }
            }
        }

        log.warn("Proceeding without MCP callbacks after {} attempts: {}", attempts,
                lastException == null ? "unknown error" : lastException.getMessage());
        return new ToolCallback[0];
    }
}
