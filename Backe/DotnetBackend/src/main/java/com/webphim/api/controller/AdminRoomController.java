package com.webphim.api.controller;

import com.webphim.api.dto.RoomDtos.*;
import com.webphim.api.service.RoomService;
import com.webphim.api.service.SeatService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

public final class AdminRoomController {
    private AdminRoomController() {}
}

@RestController
@RequestMapping("/api/admin/rooms")
@PreAuthorize("hasRole('admin')")
class AdminRoomsController {
    private final RoomService roomService;

    AdminRoomsController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    Object getPaged(@RequestParam(defaultValue = "1") int pageNumber,
                    @RequestParam(defaultValue = "10") int pageSize,
                    @RequestParam(required = false) String search) {
        return roomService.getPaged(pageNumber, pageSize, search);
    }

    @GetMapping("/all")
    Object getAll() {
        return roomService.getAll();
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id) {
        return roomService.getById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy phòng chiếu.")));
    }

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(201).body(roomService.create(request));
    }

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody CreateRoomRequest request) {
        return roomService.update(id, request).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy phòng chiếu.")));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id) {
        return roomService.delete(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy phòng chiếu."));
    }
}

@RestController
@RequestMapping("/api/admin/seats")
@PreAuthorize("hasRole('admin')")
class AdminSeatController {
    private final SeatService seatService;

    AdminSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/room/{roomId}")
    Object getByRoom(@PathVariable Integer roomId) {
        return seatService.getByRoomId(roomId);
    }

    @GetMapping("/types")
    Object getSeatTypes() {
        return seatService.getAllSeatTypes();
    }

    @PutMapping("/room/{roomId}/map")
    Object updateSeatMap(@PathVariable Integer roomId, @RequestBody UpdateSeatMapRequest request) {
        var updatedCount = seatService.updateSeatMap(roomId, request);
        return Map.of("message", "Đã cập nhật " + updatedCount + " ghế thành công.", "updatedCount", updatedCount);
    }

    @PostMapping("/room/{roomId}/generate")
    Object generateSeatMap(@PathVariable Integer roomId, @Valid @RequestBody GenerateSeatMapRequest request) {
        var createdCount = seatService.initializeSeatMap(roomId, request);
        return Map.of("message", "Đã khởi tạo thành công " + createdCount + " ghế cho phòng.", "createdCount", createdCount);
    }

    @DeleteMapping("/room/{roomId}")
    ResponseEntity<?> deleteSeatMap(@PathVariable Integer roomId) {
        return seatService.deleteAllByRoomId(roomId)
                ? ResponseEntity.ok(Map.of("message", "Đã xoá toàn bộ sơ đồ ghế thành công."))
                : ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy sơ đồ ghế."));
    }
}
