package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.utkarsh.tripmate.config.properties.CurrencyExchangeProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CurrencyExchangeService {

    private final CurrencyExchangeProperties properties;
    private final RestTemplate restTemplate;

    public CurrencyExchangeService(CurrencyExchangeProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(required = true, description = "Input amount") double amount,
            @ToolParam(required = true, description = "Input currency code") AirfareService.Currency currencyIn,
            @ToolParam(required = true, description = "Output currency code") AirfareService.Currency currencyOut) {
    }

    public record Response(
            @JsonPropertyDescription("Converted amount in output currency.") double amount,
            @JsonPropertyDescription("Output currency.") AirfareService.Currency currencyOut) {
    }

    @Tool(name = "currencyExchangeService", description = "Converts amount between currencies using VAT Comply rates")
    public Response apply(@ToolParam Request request) {
        String url = properties.getVatComply().getUrl() + "?base=" + request.currencyIn().name();
        JsonNode body = restTemplate.getForObject(url, JsonNode.class);
        JsonNode rateNode = body == null ? null : body.path("rates").path(request.currencyOut().name());
        if (rateNode == null || rateNode.isMissingNode()) {
            throw new IllegalStateException("Exchange rate not found for " + request.currencyOut());
        }
        double converted = request.amount() * rateNode.asDouble();
        return new Response(converted, request.currencyOut());
    }
}
