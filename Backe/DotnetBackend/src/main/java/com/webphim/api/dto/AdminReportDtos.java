package com.webphim.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminReportDtos {
    private AdminReportDtos() {}

    public record AdminInvoiceSeatResponse(String seatLabel, String seatType, BigDecimal price) {}

    public record AdminInvoiceResponse(
            Integer orderId,
            String orderCode,
            BigDecimal totalAmount,
            BigDecimal finalAmount,
            String status,
            String paymentMethod,
            LocalDateTime paidAt,
            String customerName,
            String customerEmail,
            String customerPhone,
            String movieTitle,
            LocalDateTime showtime,
            String roomName,
            List<AdminInvoiceSeatResponse> seats) {}

    public record RevenuePointResponse(LocalDate date, BigDecimal revenue) {}

    public record RecentCustomerResponse(Integer userId, String fullName, String email, LocalDateTime createdAt) {}

    public record AdminOverviewStatsResponse(
            BigDecimal totalRevenue,
            long totalTicketsSold,
            long newCustomersThisMonth,
            List<RevenuePointResponse> dailyRevenue,
            List<AdminInvoiceResponse> recentInvoices,
            List<RecentCustomerResponse> recentCustomers) {}
}
