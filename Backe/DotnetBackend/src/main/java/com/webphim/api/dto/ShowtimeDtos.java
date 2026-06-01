package com.webphim.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ShowtimeDtos {
    private ShowtimeDtos() {}

    public record ShowtimeDetailResponse(
            Integer showtimeId,
            Integer movieId,
            String movieTitle,
            String poster,
            String roomName,
            LocalDateTime startTime,
            BigDecimal basePrice,
            Integer duration,
            String genre,
            String ageRating) {}

    public record AdminShowtimeResponse(
            Integer showtimeId,
            Integer movieId,
            String movieTitle,
            String poster,
            Integer roomId,
            String roomName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal basePrice,
            String status,
            int soldSeats,
            int totalSeats,
            Integer duration) {}

    public record SeatStatusResponse(
            @JsonProperty("id") Integer seatId,
            String rowName,
            Integer seatNumber,
            String status,
            String seatTypeName,
            BigDecimal price) {}

    public record CreateShowtimeRequest(
            Integer movieId,
            Integer roomId,
            LocalDateTime startTime,
            BigDecimal basePrice,
            Integer bufferTimeMinutes) {
        public int bufferTimeMinutesOrDefault() { return bufferTimeMinutes == null ? 15 : bufferTimeMinutes; }
    }

    public record UpdateShowtimeRequest(
            Integer movieId,
            Integer roomId,
            LocalDateTime startTime,
            BigDecimal basePrice,
            Integer bufferTimeMinutes) {
        public int bufferTimeMinutesOrDefault() { return bufferTimeMinutes == null ? 15 : bufferTimeMinutes; }
    }

    public record AutoGenerateRequest(
            Integer movieId,
            List<Integer> roomIds,
            LocalDate date,
            LocalTime openTime,
            LocalTime closeTime,
            BigDecimal basePrice,
            Integer cleaningMinutes) {
        public int cleaningMinutesOrDefault() { return cleaningMinutes == null ? 15 : cleaningMinutes; }
    }

    public record AutoGenerateResult(int createdCount, List<String> conflicts) {}
}
