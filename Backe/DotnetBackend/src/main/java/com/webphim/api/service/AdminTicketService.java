package com.webphim.api.service;

import com.webphim.api.dto.BookingDtos.*;
import com.webphim.api.entity.BookingSeat;
import com.webphim.api.entity.OrderStatus;
import com.webphim.api.repository.BookingRepository;
import com.webphim.api.repository.BookingSeatRepository;
import com.webphim.api.repository.OrderRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTicketService {
    private final OrderRepository orders;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;

    public AdminTicketService(OrderRepository orders, BookingRepository bookings, BookingSeatRepository bookingSeats) {
        this.orders = orders;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
    }

    @Transactional
    public ScanResultResponse scanTicket(String orderCode) {
        var order = orders.findByOrderCode(orderCode).orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng '" + orderCode + "'"));
        if (order.getStatus() != OrderStatus.paid) {
            throw new IllegalStateException("Đơn hàng chưa được thanh toán (status: " + order.getStatus() + ")");
        }
        var orderBookings = bookings.findDetailsByOrderId(order.getOrderId());
        if (orderBookings.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy booking cho đơn hàng này");
        }
        var firstShowtime = orderBookings.get(0).getShowtime();
        var seatInfos = new java.util.ArrayList<SeatInfo>();
        var allSeats = new java.util.ArrayList<BookingSeat>();
        var alreadyCheckedIn = false;
        for (var booking : orderBookings) {
            var rows = bookingSeats.findByBookingWithSeat(booking.getBookingId());
            allSeats.addAll(rows);
            for (var bs : rows) {
                if (bs.isCheckedIn()) {
                    alreadyCheckedIn = true;
                }
                seatInfos.add(new SeatInfo(bs.getSeat().getRowName() + bs.getSeat().getSeatNumber(),
                        bs.getSeat().getSeatType().getName(), bs.getPrice(), bs.isCheckedIn()));
            }
        }
        if (!alreadyCheckedIn) {
            allSeats.forEach(bs -> bs.setCheckedIn(true));
        }
        return new ScanResultResponse(order.getOrderCode(), order.getStatus().name(), firstShowtime.getMovie().getTitle(),
                firstShowtime.getStartTime(), firstShowtime.getRoom().getName(), order.getFinalAmount(),
                order.getUser().getFullName(), order.getUser().getPhoneNumber(), seatInfos, alreadyCheckedIn);
    }
}
