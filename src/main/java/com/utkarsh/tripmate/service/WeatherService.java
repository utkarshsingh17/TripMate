package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.utkarsh.tripmate.config.properties.WeatherProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class WeatherService {

    private final RestTemplate restTemplate;
    private final WeatherProperties weatherProperties;

    public WeatherService(RestTemplate restTemplate, WeatherProperties weatherProperties) {
        this.restTemplate = restTemplate;
        this.weatherProperties = weatherProperties;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(description = "The city and state e.g. San Francisco, CA", required = true) String location,
            @ToolParam(description = "The city latitude", required = true) double lat,
            @ToolParam(description = "The city longitude", required = true) double lon,
            @ToolParam(description = "Temperature unit based on location country", required = true) Unit unit,
            @ToolParam(description = "The month in which the weather is desired", required = true) int month,
            @ToolParam(description = "The year in which the weather is desired", required = true) int year) {
    }

    public enum Unit {
        C("metric"), F("us");
        private final String apiUnit;

        Unit(String apiUnit) {
            this.apiUnit = apiUnit;
        }
    }

    public record Response(
            @JsonPropertyDescription("Average temperature for requested month.") double temp,
            @JsonPropertyDescription("Minimum temperature for requested month.") double tempMin,
            @JsonPropertyDescription("Maximum temperature for requested month.") double tempMax,
            @JsonPropertyDescription("Temperature unit used in response.") Unit unit) {
    }

    @Tool(name = "weatherService", description = "Gets average weather for a location and month/year using Visual Crossing")
    public Response apply(@ToolParam Request request) {
        LocalDate start = LocalDate.of(request.year(), request.month(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        String url = weatherProperties.getVisualcrossing().getUrl() + "/"
                + request.lat() + "," + request.lon() + "/"
                + start + "/" + end
                + "?unitGroup=" + request.unit().apiUnit
                + "&key=" + weatherProperties.getVisualcrossing().getApiKey();

        JsonNode body = restTemplate.getForObject(url, JsonNode.class);
        if (body == null || !body.has("days")) {
            throw new IllegalStateException("No weather data returned from Visual Crossing");
        }

        double sumAvg = 0.0;
        double minTemp = Double.MAX_VALUE;
        double maxTemp = -Double.MAX_VALUE;
        int count = 0;
        for (JsonNode day : body.get("days")) {
            if (day.has("temp")) {
                double t = day.get("temp").asDouble();
                sumAvg += t;
                count++;
            }
            if (day.has("tempmin")) {
                minTemp = Math.min(minTemp, day.get("tempmin").asDouble());
            }
            if (day.has("tempmax")) {
                maxTemp = Math.max(maxTemp, day.get("tempmax").asDouble());
            }
        }

        if (count == 0) {
            throw new IllegalStateException("No daily weather rows returned from Visual Crossing");
        }
        return new Response(sumAvg / count, minTemp, maxTemp, request.unit());
    }
}
