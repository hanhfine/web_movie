package com.webphim.api.repository;

import com.webphim.api.entity.Room;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    Page<Room> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Room> findAllByOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndRoomIdNot(String name, Integer roomId);
}
