package com.webphim.api.repository;

import com.webphim.api.entity.SeatType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatTypeRepository extends JpaRepository<SeatType, Integer> {
    List<SeatType> findAllByOrderBySeatTypeIdAsc();
}
