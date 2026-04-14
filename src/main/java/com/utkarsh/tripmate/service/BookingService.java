package com.utkarsh.tripmate.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BookingService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @ToolParam(required = true, description = "Passenger full name") String fullName,
            @ToolParam(required = true, description = "Passenger email") String emailAddress,
            @ToolParam(required = true, description = "Trip start date in yyyy-MM-dd") String startDate,
            @ToolParam(required = true, description = "Trip end date in yyyy-MM-dd") String endDate,
            @ToolParam(required = true, description = "Origin city") String origin,
            @ToolParam(required = true, description = "Destination city") String destination,
            @ToolParam(required = true, description = "Preferred airline") String airline) {
    }

    public record Response(
            @JsonPropertyDescription("Booking operation status.") String status,
            @JsonPropertyDescription("Generated booking confirmation id.") String confirmationId) {
    }

    @Tool(name = "bookingService", description = "Performs booking operations for planned trips")
    public Response apply(@ToolParam Request request) {
        String confirmationId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new Response("Success", confirmationId);
    }
}
