package com.webphim.api.service;

import com.webphim.api.dto.BookingDtos.*;
import com.webphim.api.entity.*;
import com.webphim.api.repository.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final int BOOKING_TTL_MINUTES = 10;

    private final ShowtimeRepository showtimes;
    private final UserRepository users;
    private final SeatRepository seats;
    private final OrderRepository orders;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final StringRedisTemplate redis;

    public BookingService(ShowtimeRepository showtimes, UserRepository users, SeatRepository seats,
                          OrderRepository orders, BookingRepository bookings, BookingSeatRepository bookingSeats,
                          StringRedisTemplate redis) {
        this.showtimes = showtimes;
        this.users = users;
        this.seats = seats;
        this.orders = orders;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.redis = redis;
    }

    @Transactional
    public CreateBookingResponse bookSeats(BookingRequest request) {
        if (request.seatIds() == null || request.seatIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 ghế");
        }
        var showtime = showtimes.findDetailById(request.showtimeId())
                .orElseThrow(() -> new NoSuchElementException("Suất chiếu không tồn tại"));
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Suất chiếu này đã bắt đầu hoặc đã kết thúc, không thể đặt vé.");
        }
        var user = users.findById(request.userId()).orElseThrow(() -> new NoSuchElementException("Người dùng không tồn tại"));
        var redisHashKey = "showtime:" + request.showtimeId() + ":seats";
        var lockKeys = request.seatIds().stream().map(k -> "lock:showtime:" + request.showtimeId() + ":seat:" + k).toList();
        var acquired = new java.util.ArrayList<String>();

        try {
            for (String lockKey : lockKeys) {
                Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
                if (!Boolean.TRUE.equals(locked)) {
                    throw new IllegalStateException("Ghế đang được người khác giữ, vui lòng thử lại");
                }
                acquired.add(lockKey);
            }
            for (String seatKey : request.seatIds()) {
                Object status = redis.opsForHash().get(redisHashKey, seatKey);
                if (status != null) {
                    throw new IllegalStateException("Ghế " + seatKey + " đã được đặt hoặc đang bị giữ");
                }
            }

            var roomSeats = seats.findByRoomRoomIdOrderByRowNameAscSeatNumberAsc(showtime.getRoom().getRoomId()).stream()
                    .filter(s -> request.seatIds().contains(s.getRowName() + s.getSeatNumber()))
                    .toList();
            if (roomSeats.size() != request.seatIds().size()) {
                throw new IllegalStateException("Một hoặc nhiều ghế không tồn tại trong phòng này");
            }

            var totalAmount = roomSeats.stream()
                    .map(s -> showtime.getBasePrice().add(s.getSeatType().getSurcharge()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var pointsToUse = request.pointsToUse();
            if (pointsToUse > 0) {
                if (user.getPoints() < pointsToUse) {
                    throw new IllegalStateException("Số điểm muốn sử dụng vượt quá số dư hiện tại");
                }
                user.setPoints(user.getPoints() - pointsToUse);
            }
            var discount = BigDecimal.valueOf(pointsToUse).multiply(BigDecimal.valueOf(1000));
            var finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);

            var order = new Order();
            order.setOrderCode("TEMP");
            order.setUser(user);
            order.setTotalAmount(totalAmount);
            order.setDiscountAmount(discount);
            order.setPointsUsed(pointsToUse);
            order.setPointsEarned(0);
            order.setFinalAmount(finalAmount);
            order.setStatus(OrderStatus.pending);
            order.setPaymentMethod("QR");
            order.setExpiredAt(LocalDateTime.now().plusMinutes(BOOKING_TTL_MINUTES));
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orders.saveAndFlush(order);
            order.setOrderCode("DH" + order.getOrderId());

            var booking = new Booking();
            booking.setOrder(order);
            booking.setShowtime(showtime);
            bookings.saveAndFlush(booking);

            var savedSeats = new java.util.ArrayList<BookingSeat>();
            for (Seat seat : roomSeats) {
                var bs = new BookingSeat();
                bs.setBooking(booking);
                bs.setShowtime(showtime);
                bs.setSeat(seat);
                bs.setPrice(showtime.getBasePrice().add(seat.getSeatType().getSurcharge()));
                savedSeats.add(bs);
            }
            bookingSeats.saveAll(savedSeats);

            request.seatIds().forEach(seatKey -> redis.opsForHash().put(redisHashKey, seatKey, "LOCKED"));
            log.info("Booking thành công: bookingId={}, orderCode={}", booking.getBookingId(), order.getOrderCode());
            return new CreateBookingResponse(booking.getBookingId(), order.getOrderCode(), order.getStatus().name());
        } finally {
            acquired.forEach(redis::delete);
        }
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryItemResponse> getBookingHistory(Integer userId) {
        return orders.findByUserUserIdAndStatusOrderByCreatedAtDesc(userId, OrderStatus.paid).stream()
                .flatMap(order -> bookings.findDetailsByOrderId(order.getOrderId()).stream()
                        .findFirst()
                        .stream()
                        .map(booking -> {
                            var seatLabels = bookingSeats.findByBookingWithSeat(booking.getBookingId()).stream()
                                    .map(bs -> bs.getSeat().getRowName() + bs.getSeat().getSeatNumber())
                                    .toList();
                            var showtime = booking.getShowtime();
                            return new BookingHistoryItemResponse(order.getOrderCode(), order.getStatus().name(),
                                    showtime.getMovie().getTitle(), showtime.getMovie().getPoster() == null ? "" : showtime.getMovie().getPoster(),
                                    showtime.getStartTime(), showtime.getRoom().getName(), order.getFinalAmount(),
                                    seatLabels, order.getCreatedAt());
                        }))
                .toList();
    }
}
