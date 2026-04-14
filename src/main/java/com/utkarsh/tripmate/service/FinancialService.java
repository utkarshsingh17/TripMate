package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class FinancialService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(required = true, description = "Account number for balance lookup") String accountNumber) {
    }

    public record Response(
            @JsonPropertyDescription("Current bank balance in USD.") double bankBalance,
            @JsonPropertyDescription("Monthly income in USD.") double monthlyIncome,
            @JsonPropertyDescription("Estimated safe monthly travel budget in USD.") double suggestedTravelBudget) {
    }

    @Tool(name = "financialService", description = "Returns budget context for trip planning")
    public Response apply(@ToolParam Request request) {
        double balance = 4500.00;
        double income = 1000.00;
        double travelBudget = (balance * 0.2) + (income * 0.3);
        return new Response(balance, income, travelBudget);
    }
}
