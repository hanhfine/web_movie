package com.webphim.api.repository;

import com.webphim.api.entity.Seat;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Integer> {
    long countByRoomRoomId(Integer roomId);
    boolean existsByRoomRoomId(Integer roomId);
    List<Seat> findByRoomRoomIdOrderByRowNameAscSeatNumberAsc(Integer roomId);
    List<Seat> findByRoomRoomIdAndSeatIdIn(Integer roomId, Collection<Integer> seatIds);
    List<Seat> findByRoomRoomId(Integer roomId);

    @Query("select s from Seat s join fetch s.seatType where s.room.roomId = :roomId and concat(s.rowName, str(s.seatNumber)) in :labels")
    List<Seat> findByRoomAndLabels(@Param("roomId") Integer roomId, @Param("labels") Collection<String> labels);
}
