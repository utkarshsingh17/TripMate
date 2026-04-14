package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.utkarsh.tripmate.config.properties.FlightProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class AirfareService {

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

    @Tool(name = "airfareService", description = "Computes estimated airfare between origin and destination using Aviationstack data")
    public Response getAirfare(@ToolParam Request request) {
        String originCode = getClosestAirportIata(request.originLatitude(), request.originLongitude());
        String destinationCode = getClosestAirportIata(request.destinationLatitude(), request.destinationLongitude());
        double fare = estimateFareFromAviationData(
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                originCode,
                destinationCode);
        return new Response(fare, request.currency());
    }

    private String getClosestAirportIata(double lat, double lon) {
        String url = flightProperties.getAviationstack().getUrlAirports()
                + "?access_key=" + flightProperties.getAviationstack().getAccessKey()
                + "&limit=100";
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, null, JsonNode.class);
        JsonNode data = response.getBody() == null ? null : response.getBody().get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            throw new IllegalStateException("No airport found for coordinates");
        }

        String bestCode = null;
        double bestDistance = Double.MAX_VALUE;
        for (JsonNode airport : data) {
            JsonNode latNode = airport.get("latitude");
            JsonNode lonNode = airport.get("longitude");
            String iata = getTextValue(airport, "iata_code");
            if (latNode == null || lonNode == null || iata == null || iata.isBlank()) {
                continue;
            }
            double distance = haversineMiles(lat, lon, latNode.asDouble(), lonNode.asDouble());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestCode = iata;
            }
        }
        if (bestCode == null) {
            throw new IllegalStateException("No IATA code found in Aviationstack airport lookup");
        }
        return bestCode;
    }

    private double estimateFareFromAviationData(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            String originCode,
            String destinationCode) {
        double distanceMiles = haversineMiles(originLat, originLon, destinationLat, destinationLon);
        double baseFare = 65.0 + (distanceMiles * 0.18);
        double scheduleAdjustment = getScheduleAdjustment(originCode, destinationCode);
        return Math.round((baseFare + scheduleAdjustment) * 100.0) / 100.0;
    }

    private double getScheduleAdjustment(String originCode, String destinationCode) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.toLocalDate().toString();
        String url = flightProperties.getAviationstack().getUrlFlights()
                + "?access_key=" + flightProperties.getAviationstack().getAccessKey()
                + "&dep_iata=" + originCode
                + "&arr_iata=" + destinationCode
                + "&flight_date=" + date
                + "&limit=20";
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, null, JsonNode.class);
        JsonNode flights = response.getBody() == null ? null : response.getBody().get("data");
        if (flights == null || !flights.isArray() || flights.isEmpty()) {
            return 25.0;
        }

        int activeFlights = 0;
        for (JsonNode flight : flights) {
            String status = getTextValue(flight, "flight_status");
            if (status != null && (status.equalsIgnoreCase("scheduled") || status.equalsIgnoreCase("active"))) {
                activeFlights++;
            }
        }
        return activeFlights >= 8 ? 40.0 : activeFlights >= 3 ? 20.0 : 10.0;
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private double haversineMiles(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusMiles = 3958.7613;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMiles * c;
    }
}
