package com.webphim.api.repository;

import com.webphim.api.entity.Showtime;
import com.webphim.api.entity.ShowtimeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {
    @Query("select s from Showtime s join fetch s.movie join fetch s.room where s.status = :status and s.movie.deleted = false")
    List<Showtime> findClientShowtimes(@Param("status") ShowtimeStatus status);

    @Query("select s from Showtime s join fetch s.movie join fetch s.room where s.movie.movieId = :movieId and s.status = :status")
    List<Showtime> findClientShowtimesByMovie(@Param("movieId") Integer movieId, @Param("status") ShowtimeStatus status);

    @Query("select s from Showtime s join fetch s.movie join fetch s.room where s.showtimeId = :id")
    Optional<Showtime> findDetailById(@Param("id") Integer id);

    @Query("select s from Showtime s join fetch s.movie join fetch s.room where (:start is null or s.startTime >= :start) and (:end is null or s.startTime <= :end) and (:roomId is null or s.room.roomId = :roomId)")
    List<Showtime> findAdminShowtimes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("roomId") Integer roomId);

    boolean existsByRoomRoomId(Integer roomId);

    @Query("select s from Showtime s where s.room.roomId = :roomId and s.status in (com.webphim.api.entity.ShowtimeStatus.active, com.webphim.api.entity.ShowtimeStatus.draft) and :start < s.endTime and :end > s.startTime and (:excludeId is null or s.showtimeId <> :excludeId)")
    Optional<Showtime> findOverlap(@Param("roomId") Integer roomId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("excludeId") Integer excludeId);
}
