package com.webphim.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class RoomDtos {
    private RoomDtos() {}

    public record RoomResponse(
            @JsonProperty("id") Integer roomId,
            String name,
            Integer totalSeats,
            String status) {}

    public record RoomPagedResponse(
            List<RoomResponse> items,
            long totalCount,
            int pageNumber,
            int pageSize,
            int totalPages) {}

    public record CreateRoomRequest(
            @NotBlank @Size(max = 50) String name,
            String status) {
        public String statusOrDefault() { return status == null || status.isBlank() ? "active" : status; }
    }

    public record SeatTypeResponse(
            Integer seatTypeId,
            String name,
            BigDecimal surcharge) {}

    public record SeatResponse(
            Integer seatId,
            Integer roomId,
            String rowName,
            Integer seatNumber,
            SeatTypeResponse seatType) {}

    public record SeatUpdateItem(Integer seatId, Integer seatTypeId) {}

    public record UpdateSeatMapRequest(List<SeatUpdateItem> seats) {}

    public record GenerateSeatMapRequest(
            @Min(1) @Max(26) int rows,
            @Min(1) @Max(100) int columns) {}
}
