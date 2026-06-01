package com.webphim.api.service;

import com.webphim.api.dto.BookingDtos.*;
import com.webphim.api.entity.*;
import com.webphim.api.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBookingService {
    private static final Logger log = LoggerFactory.getLogger(AdminBookingService.class);
    private final ShowtimeRepository showtimes;
    private final UserRepository users;
    private final SeatRepository seats;
    private final OrderRepository orders;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final StringRedisTemplate redis;

    public AdminBookingService(ShowtimeRepository showtimes, UserRepository users, SeatRepository seats,
                               OrderRepository orders, BookingRepository bookings,
                               BookingSeatRepository bookingSeats, StringRedisTemplate redis) {
        this.showtimes = showtimes;
        this.users = users;
        this.seats = seats;
        this.orders = orders;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.redis = redis;
    }

    @Transactional
    public AdminBookingResponse bookTicketsAtCounter(AdminBookingRequest request) {
        var showtime = showtimes.findDetailById(request.showtimeId()).orElseThrow(() -> new NoSuchElementException("Suất chiếu không tồn tại"));
        var user = users.findById(request.userId() == null ? 1 : request.userId()).orElseThrow(() -> new NoSuchElementException("Người dùng không tồn tại"));
        var redisKey = "showtime:" + request.showtimeId() + ":seats";
        for (String seatKey : request.seatIds()) {
            var status = redis.opsForHash().get(redisKey, seatKey);
            if (status != null && !"AVAILABLE".equals(status.toString())) {
                throw new IllegalStateException("Ghế " + seatKey + " đã được đặt");
            }
        }
        var selectedSeats = seats.findByRoomRoomIdOrderByRowNameAscSeatNumberAsc(showtime.getRoom().getRoomId()).stream()
                .filter(s -> request.seatIds().contains(s.getRowName() + s.getSeatNumber()))
                .toList();
        var totalAmount = selectedSeats.stream()
                .map(s -> showtime.getBasePrice().add(s.getSeatType().getSurcharge()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var order = new Order();
        order.setOrderCode("TEMP");
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.paid);
        order.setPaymentMethod(request.paymentMethodOrDefault());
        order.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orders.saveAndFlush(order);
        order.setOrderCode("DH" + order.getOrderId());
        var booking = new Booking();
        booking.setOrder(order);
        booking.setShowtime(showtime);
        bookings.saveAndFlush(booking);
        var bookingSeatRows = selectedSeats.stream().map(seat -> {
            var bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setShowtime(showtime);
            bs.setSeat(seat);
            bs.setPrice(showtime.getBasePrice().add(seat.getSeatType().getSurcharge()));
            return bs;
        }).toList();
        bookingSeats.saveAll(bookingSeatRows);
        request.seatIds().forEach(seatKey -> redis.opsForHash().put(redisKey, seatKey, "SOLD"));
        log.info("Admin tạo vé tại quầy: orderCode={}", order.getOrderCode());
        return new AdminBookingResponse(booking.getBookingId(), order.getOrderCode(), order.getStatus().name(), request.paymentMethodOrDefault());
    }
}
