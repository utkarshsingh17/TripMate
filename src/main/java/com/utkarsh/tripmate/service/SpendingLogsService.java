package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SpendingLogsService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(required = true, description = "Start date in yyyy-MM-dd") String startDate,
            @ToolParam(required = true, description = "End date in yyyy-MM-dd") String endDate) {
    }

    public record Response(
            @JsonPropertyDescription("Purchase description.") String description,
            @JsonPropertyDescription("Expense category.") String category,
            @JsonPropertyDescription("Transaction date.") LocalDate dateOfTransaction,
            @JsonPropertyDescription("Transaction amount in USD.") Double amountOfTransaction) {
    }

    @Tool(name = "spendingLogsService", description = "Returns spending history for expense tracking")
    public List<Response> apply(@ToolParam Request request) {
        return List.of(
                new Response("Hotel dinner", "Dining", LocalDate.now().minusDays(10), 42.50),
                new Response("Airport taxi", "Transportation", LocalDate.now().minusDays(9), 28.00),
                new Response("Museum tickets", "Entertainment", LocalDate.now().minusDays(7), 35.00),
                new Response("Groceries", "Grocery", LocalDate.now().minusDays(4), 64.20));
    }
}
