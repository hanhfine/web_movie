package com.webphim.api.controller;

import com.webphim.api.entity.ShowtimeStatus;
import com.webphim.api.repository.RoomRepository;
import com.webphim.api.repository.SeatTypeRepository;
import com.webphim.api.repository.ShowtimeRepository;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomRepository rooms;
    private final SeatTypeRepository seatTypes;
    private final ShowtimeRepository showtimes;

    public RoomController(RoomRepository rooms, SeatTypeRepository seatTypes, ShowtimeRepository showtimes) {
        this.rooms = rooms;
        this.seatTypes = seatTypes;
        this.showtimes = showtimes;
    }

    @GetMapping
    public Object getAll() {
        return rooms.findAll().stream().map(r -> java.util.Map.of("roomId", r.getRoomId(), "name", r.getName())).toList();
    }

    @GetMapping("/seat-types")
    public Object getSeatTypes() {
        return seatTypes.findAllByOrderBySeatTypeIdAsc().stream()
                .map(s -> java.util.Map.of("seatTypeId", s.getSeatTypeId(), "name", s.getName(), "surcharge", s.getSurcharge()))
                .toList();
    }

    @GetMapping("/pricing")
    public Object getRoomPricing() {
        var activeShowtimes = showtimes.findClientShowtimes(ShowtimeStatus.active);
        return rooms.findAll().stream().map(room -> {
            var basePrice = activeShowtimes.stream()
                    .filter(s -> s.getRoom().getRoomId().equals(room.getRoomId()))
                    .map(s -> s.getBasePrice())
                    .findFirst()
                    .orElse(BigDecimal.valueOf(75000));
            return java.util.Map.of("roomId", room.getRoomId(), "name", room.getName(), "basePrice", basePrice);
        }).toList();
    }
}
