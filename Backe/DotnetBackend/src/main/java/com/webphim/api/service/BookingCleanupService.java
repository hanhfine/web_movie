package com.webphim.api.service;

import com.webphim.api.entity.OrderStatus;
import com.webphim.api.repository.BookingRepository;
import com.webphim.api.repository.BookingSeatRepository;
import com.webphim.api.repository.OrderRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingCleanupService {
    private static final Logger log = LoggerFactory.getLogger(BookingCleanupService.class);
    private final OrderRepository orders;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final StringRedisTemplate redis;

    public BookingCleanupService(OrderRepository orders, BookingRepository bookings, BookingSeatRepository bookingSeats,
                                 StringRedisTemplate redis) {
        this.orders = orders;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.redis = redis;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void cleanupExpiredBookings() {
        var expiredOrders = orders.findByStatusAndExpiredAtBefore(OrderStatus.pending, LocalDateTime.now());
        if (expiredOrders.isEmpty()) {
            return;
        }
        for (var order : expiredOrders) {
            order.setStatus(OrderStatus.cancelled);
            order.setUpdatedAt(LocalDateTime.now());
            for (var booking : bookings.findByOrderOrderId(order.getOrderId())) {
                var redisKey = "showtime:" + booking.getShowtime().getShowtimeId() + ":seats";
                var rows = bookingSeats.findByBookingWithSeat(booking.getBookingId());
                for (var bs : rows) {
                    var seatKey = bs.getSeat().getRowName() + bs.getSeat().getSeatNumber();
                    var status = redis.opsForHash().get(redisKey, seatKey);
                    if ("LOCKED".equals(status)) {
                        redis.opsForHash().delete(redisKey, seatKey);
                    }
                }
                bookingSeats.deleteAll(rows);
            }
        }
        log.info("Đã dọn {} đơn hàng hết hạn", expiredOrders.size());
    }
}
