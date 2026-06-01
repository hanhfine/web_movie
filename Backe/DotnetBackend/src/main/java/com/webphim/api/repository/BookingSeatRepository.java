package com.webphim.api.repository;

import com.webphim.api.entity.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Integer> {
    @Query("select bs from BookingSeat bs join fetch bs.seat seat join fetch seat.seatType where bs.booking.bookingId = :bookingId")
    List<BookingSeat> findByBookingWithSeat(@Param("bookingId") Integer bookingId);

    long countByBookingShowtimeShowtimeId(Integer showtimeId);

    @Query("select count(bs) from BookingSeat bs where bs.booking.order.status = com.webphim.api.entity.OrderStatus.paid and (:fromDate is null or bs.booking.order.createdAt >= :fromDate) and (:toDate is null or bs.booking.order.createdAt <= :toDate)")
    long countPaidTickets(@Param("fromDate") java.time.LocalDateTime fromDate, @Param("toDate") java.time.LocalDateTime toDate);
}
