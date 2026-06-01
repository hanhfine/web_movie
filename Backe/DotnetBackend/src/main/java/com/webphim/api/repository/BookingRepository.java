package com.webphim.api.repository;

import com.webphim.api.entity.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByOrderOrderId(Integer orderId);

    @Query("select b from Booking b join fetch b.showtime s join fetch s.movie join fetch s.room where b.order.orderId = :orderId")
    List<Booking> findDetailsByOrderId(@Param("orderId") Integer orderId);
}
