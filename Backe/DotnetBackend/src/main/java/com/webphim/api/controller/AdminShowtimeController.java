package com.webphim.api.controller;

import com.webphim.api.dto.ShowtimeDtos.*;
import com.webphim.api.service.ShowtimeService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/showtimes")
@PreAuthorize("hasRole('admin')")
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;

    public AdminShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    Object getAll(@RequestParam(required = false) LocalDate date,
                  @RequestParam(required = false) Integer roomId) {
        return showtimeService.getAdminShowtimes(date, roomId);
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id) {
        return showtimeService.getDetail(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    ResponseEntity<?> create(@RequestBody CreateShowtimeRequest request) {
        return ResponseEntity.status(201).body(showtimeService.create(request));
    }

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @RequestBody UpdateShowtimeRequest request) {
        return showtimeService.update(id, request).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> cancel(@PathVariable Integer id) {
        return showtimeService.cancel(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/publish")
    ResponseEntity<?> publish(@PathVariable Integer id) {
        return showtimeService.publish(id)
                ? ResponseEntity.ok(Map.of("message", "Đã xuất bản suất chiếu lên website."))
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/auto-generate")
    ResponseEntity<?> autoGenerate(@RequestBody AutoGenerateRequest request) {
        if (request.roomIds() == null || request.roomIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phải chọn ít nhất 1 phòng chiếu."));
        }
        if (!request.openTime().isBefore(request.closeTime())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Giờ mở cửa phải trước giờ đóng cửa."));
        }
        var result = showtimeService.autoGenerate(request);
        return ResponseEntity.ok(Map.of("message", "Đã tạo " + result.createdCount() + " suất chiếu.",
                "createdCount", result.createdCount(), "conflicts", result.conflicts()));
    }
}
