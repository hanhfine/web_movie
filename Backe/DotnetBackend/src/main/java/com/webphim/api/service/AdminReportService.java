package com.webphim.api.service;

import com.webphim.api.dto.AdminReportDtos.*;
import com.webphim.api.entity.Order;
import com.webphim.api.entity.UserRole;
import com.webphim.api.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {
    private final OrderRepository orders;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final UserRepository users;

    public AdminReportService(OrderRepository orders, BookingRepository bookings,
                              BookingSeatRepository bookingSeats, UserRepository users) {
        this.orders = orders;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AdminInvoiceResponse> getPaidInvoices(LocalDateTime fromDate, LocalDateTime toDate, String searchTerm,
                                                      BigDecimal minAmount, BigDecimal maxAmount) {
        var search = searchTerm == null || searchTerm.isBlank() ? null : searchTerm.trim();
        return orders.searchPaidInvoices(fromDate, toDate, search, minAmount, maxAmount).stream().map(this::toInvoice).toList();
    }

    @Transactional(readOnly = true)
    public AdminOverviewStatsResponse getOverview(LocalDateTime from, LocalDateTime to) {
        var revenue = orders.sumRevenue(from, to);
        var ticketCount = bookingSeats.countPaidTickets(from, to);
        var monthStart = YearMonth.now().atDay(1).atStartOfDay();
        var newCustomers = users.countByRoleAndCreatedAtGreaterThanEqual(UserRole.customer, monthStart);
        var dailyRevenue = orders.paidOrdersForRevenue(from, to).stream()
                .collect(java.util.stream.Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Order::getFinalAmount, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new RevenuePointResponse(e.getKey(), e.getValue()))
                .toList();
        var recentInvoices = getPaidInvoices(from, to, null, null, null).stream().limit(6).toList();
        var recentCustomers = users.findTop6ByRoleOrderByCreatedAtDesc(UserRole.customer).stream()
                .map(u -> new RecentCustomerResponse(u.getUserId(), u.getFullName(), u.getEmail(), u.getCreatedAt()))
                .toList();
        return new AdminOverviewStatsResponse(revenue, ticketCount, newCustomers, dailyRevenue, recentInvoices, recentCustomers);
    }

    private AdminInvoiceResponse toInvoice(Order order) {
        var firstBooking = bookings.findDetailsByOrderId(order.getOrderId()).stream().findFirst().orElse(null);
        var seatRows = firstBooking == null ? List.<AdminInvoiceSeatResponse>of()
                : bookingSeats.findByBookingWithSeat(firstBooking.getBookingId()).stream()
                    .map(bs -> new AdminInvoiceSeatResponse(bs.getSeat().getRowName() + bs.getSeat().getSeatNumber(),
                            bs.getSeat().getSeatType().getName(), bs.getPrice()))
                    .toList();
        var movieTitle = firstBooking == null ? "Không xác định" : firstBooking.getShowtime().getMovie().getTitle();
        var showtime = firstBooking == null ? order.getCreatedAt() : firstBooking.getShowtime().getStartTime();
        var room = firstBooking == null ? "Không xác định" : firstBooking.getShowtime().getRoom().getName();
        return new AdminInvoiceResponse(order.getOrderId(), order.getOrderCode(), order.getTotalAmount(), order.getFinalAmount(),
                order.getStatus().name(), order.getPaymentMethod(), order.getUpdatedAt(),
                order.getUser().getFullName(), order.getUser().getEmail(), order.getUser().getPhoneNumber(),
                movieTitle, showtime, room, seatRows);
    }
}
