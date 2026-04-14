package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.utkarsh.tripmate.config.properties.FlightProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class AirfareService {

    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private final FlightProperties flightProperties;
    private final RestTemplate restTemplate;

    public AirfareService(FlightProperties flightProperties, RestTemplate restTemplate) {
        this.flightProperties = flightProperties;
        this.restTemplate = restTemplate;
    }

    public enum Currency {
        USD, GBP, EUR, INR, JPY, CAD
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(description = "Origin city/state", required = true) String origin,
            @ToolParam(description = "Destination city/state", required = true) String destination,
            @ToolParam(description = "Expected fare currency", required = true) Currency currency,
            @ToolParam(description = "Origin latitude", required = true) double originLatitude,
            @ToolParam(description = "Origin longitude", required = true) double originLongitude,
            @ToolParam(description = "Destination latitude", required = true) double destinationLatitude,
            @ToolParam(description = "Destination longitude", required = true) double destinationLongitude,
            @ToolParam(description = "Travel month", required = true) int month,
            @ToolParam(description = "Travel year", required = true) int year) {
    }

    public record Response(
            @JsonPropertyDescription("Best available airfare.") double airfare,
            @JsonPropertyDescription("Currency of returned airfare.") Currency currency) {
    }

    @Tool(name = "airfareService", description = "Computes airfare between origin and destination using Amadeus APIs")
    public Response getAirfare(@ToolParam Request request) {
        String originCode = getClosestAirportIata(request.originLatitude(), request.originLongitude());
        String destinationCode = getClosestAirportIata(request.destinationLatitude(), request.destinationLongitude());
        double fare = fetchLowestFare(originCode, destinationCode, request.month(), request.year());
        return new Response(fare, request.currency());
    }

    private String getClosestAirportIata(double lat, double lon) {
        String url = flightProperties.getAmadeus().getUrlReferenceAirports()
                + "?latitude=" + lat + "&longitude=" + lon;
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(buildAuthHeader()), JsonNode.class);
        JsonNode data = response.getBody() == null ? null : response.getBody().get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            throw new IllegalStateException("No airport found for coordinates");
        }
        JsonNode code = data.get(0).get("iataCode");
        if (code == null || code.asText().isBlank()) {
            throw new IllegalStateException("No IATA code found in airport lookup");
        }
        return code.asText();
    }

    private double fetchLowestFare(String originCode, String destinationCode, int month, int year) {
        LocalDate midMonth = LocalDate.of(year, month, 15);
        String url = flightProperties.getAmadeus().getUrlShopping()
                + "?originLocationCode=" + originCode
                + "&destinationLocationCode=" + destinationCode
                + "&departureDate=" + midMonth
                + "&adults=1&max=10";
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(buildAuthHeader()), JsonNode.class);
        JsonNode offers = response.getBody() == null ? null : response.getBody().get("data");
        if (offers == null || !offers.isArray() || offers.isEmpty()) {
            throw new IllegalStateException("No flight offers returned by Amadeus");
        }

        double minFare = Double.MAX_VALUE;
        for (JsonNode offer : offers) {
            JsonNode total = offer.path("price").path("total");
            if (!total.isMissingNode()) {
                minFare = Math.min(minFare, total.asDouble());
            }
        }
        if (minFare == Double.MAX_VALUE) {
            throw new IllegalStateException("Unable to parse fares from Amadeus response");
        }
        return minFare;
    }

    private String getBearerToken() {
        String url = flightProperties.getAmadeus().getUrlSecurity();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        body.add("client_id", flightProperties.getAmadeus().getClientId());
        body.add("client_secret", flightProperties.getAmadeus().getClientSecret());

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        JsonNode token = response.getBody() == null ? null : response.getBody().get("access_token");
        if (token == null || token.asText().isBlank()) {
            throw new IllegalStateException("Unable to obtain Amadeus OAuth token");
        }
        return token.asText();
    }

    private HttpHeaders buildAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getBearerToken());
        return headers;
    }
}
