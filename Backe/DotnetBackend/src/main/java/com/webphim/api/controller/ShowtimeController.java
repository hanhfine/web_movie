package com.webphim.api.controller;

import com.webphim.api.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    public Object getAll() {
        return showtimeService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        return showtimeService.getDetail(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/movie/{movieId}")
    public Object getByMovie(@PathVariable Integer movieId) {
        return showtimeService.getByMovie(movieId);
    }

    @GetMapping("/{id}/seats")
    public Object getSeatStatuses(@PathVariable Integer id) {
        return showtimeService.getSeatStatuses(id);
    }
}
