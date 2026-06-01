package com.webphim.api.service;

import com.webphim.api.dto.BookingDtos.BookingStatusResponse;
import com.webphim.api.dto.PaymentDtos.SePayWebhookRequest;
import com.webphim.api.entity.Order;
import com.webphim.api.entity.OrderStatus;
import com.webphim.api.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("(?i)D[H]?(\\d+)");

    private final OrderRepository orders;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final StringRedisTemplate redis;

    public PaymentService(OrderRepository orders, UserRepository users, BookingRepository bookings,
                          BookingSeatRepository bookingSeats, StringRedisTemplate redis) {
        this.orders = orders;
        this.users = users;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.redis = redis;
    }

    @Transactional
    public void processWebhook(SePayWebhookRequest request) {
        var order = resolveOrder(request).orElse(null);
        if (order == null) {
            log.warn("Không thể xác định Order cho webhook.");
            return;
        }
        if (order.getStatus() != OrderStatus.pending) {
            return;
        }
        if (request.transferAmount() != null && request.transferAmount().compareTo(order.getFinalAmount()) >= 0) {
            order.setStatus(OrderStatus.paid);
            order.setUpdatedAt(LocalDateTime.now());
            users.findById(order.getUser().getUserId()).ifPresent(user -> {
                int earnedPoints = order.getFinalAmount().multiply(BigDecimal.valueOf(0.05))
                        .divideToIntegralValue(BigDecimal.valueOf(1000)).intValue();
                order.setPointsEarned(earnedPoints);
                user.setPoints(user.getPoints() + earnedPoints);
            });
            processSeatsOnPaymentSuccess(order);
        }
    }

    @Transactional(readOnly = true)
    public BookingStatusResponse getBookingStatus(String orderCode) {
        var order = orders.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return new BookingStatusResponse(null, "not_found", false);
        }
        var bookingId = bookings.findByOrderOrderId(order.getOrderId()).stream().findFirst().map(b -> b.getBookingId()).orElse(null);
        return new BookingStatusResponse(bookingId, order.getStatus().name(), order.getStatus() == OrderStatus.paid);
    }

    private Optional<Order> resolveOrder(SePayWebhookRequest request) {
        var orderCode = extractOrderCode(request.content());
        if (orderCode != null) {
            var order = orders.findByOrderCode(orderCode);
            if (order.isPresent()) {
                return order;
            }
        }
        var cutoff = LocalDateTime.now().minusMinutes(15);
        var candidates = orders.findByStatusAndFinalAmountAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                OrderStatus.pending, request.transferAmount(), cutoff);
        return candidates.stream().findFirst();
    }

    private void processSeatsOnPaymentSuccess(Order order) {
        for (var booking : bookings.findByOrderOrderId(order.getOrderId())) {
            var redisKey = "showtime:" + booking.getShowtime().getShowtimeId() + ":seats";
            for (var bs : bookingSeats.findByBookingWithSeat(booking.getBookingId())) {
                redis.opsForHash().put(redisKey, bs.getSeat().getRowName() + bs.getSeat().getSeatNumber(), "SOLD");
            }
        }
    }

    private static String extractOrderCode(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        var matcher = ORDER_CODE_PATTERN.matcher(content);
        return matcher.find() ? "DH" + matcher.group(1) : null;
    }
}
