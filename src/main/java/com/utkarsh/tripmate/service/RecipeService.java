package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class RecipeService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(description = "Dish name for dietary recommendations", required = true) String dishName) {
    }

    public record Response(
            @JsonPropertyDescription("Protein percentage.") double proteinPercent,
            @JsonPropertyDescription("Carbohydrate percentage.") double carbPercent,
            @JsonPropertyDescription("Fat percentage.") double fatPercent,
            @JsonPropertyDescription("Estimated calories per serving.") double calories,
            @JsonPropertyDescription("Estimated cost per serving.") double cost) {
    }

    @Tool(name = "recipeService", description = "Returns nutrition split and meal estimate for requested dish")
    public Response apply(@ToolParam Request request) {
        List<List<Double>> dietProfiles = List.of(
                List.of(35.0, 35.0, 30.0, 520.0, 11.0),
                List.of(30.0, 40.0, 30.0, 610.0, 14.0),
                List.of(40.0, 30.0, 30.0, 480.0, 13.5),
                List.of(25.0, 45.0, 30.0, 700.0, 9.5));
        List<Double> profile = dietProfiles.get(new Random().nextInt(dietProfiles.size()));
        return new Response(profile.get(0), profile.get(1), profile.get(2), profile.get(3), profile.get(4));
    }
}
