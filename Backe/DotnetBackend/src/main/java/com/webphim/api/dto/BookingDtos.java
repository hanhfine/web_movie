package com.webphim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {
    private BookingDtos() {}

    public record BookingRequest(
            @NotNull Integer userId,
            @NotNull Integer showtimeId,
            @NotEmpty List<String> seatIds,
            int pointsToUse) {}

    public record CreateBookingResponse(
            Integer bookingId,
            String orderCode,
            String status) {}

    public record BookingStatusResponse(
            Integer bookingId,
            String status,
            boolean paid) {}

    public record BookingHistoryItemResponse(
            String orderCode,
            String status,
            String movieTitle,
            String moviePoster,
            LocalDateTime showtime,
            String room,
            BigDecimal totalAmount,
            List<String> seats,
            LocalDateTime createdAt) {}

    public record AdminBookingRequest(
            Integer userId,
            @NotNull Integer showtimeId,
            @NotEmpty List<String> seatIds,
            String paymentMethod) {
        public String paymentMethodOrDefault() {
            return paymentMethod == null || paymentMethod.isBlank() ? "CASH" : paymentMethod;
        }
    }

    public record AdminBookingResponse(
            Integer bookingId,
            String orderCode,
            String status,
            String paymentMethod) {}

    public record TicketScanRequest(@NotBlank String orderCode) {}

    public record ScanResultResponse(
            String orderCode,
            String status,
            String movieTitle,
            LocalDateTime showtime,
            String room,
            BigDecimal totalAmount,
            String customerName,
            String customerPhone,
            List<SeatInfo> seats,
            boolean isFullyCheckedIn) {}

    public record SeatInfo(
            String seatLabel,
            String seatType,
            BigDecimal price,
            boolean isCheckedIn) {}
}
