package com.webphim.api.controller;

import com.webphim.api.dto.MovieDtos.CreateMovieRequest;
import com.webphim.api.service.MovieService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(status == null ? movieService.getAll() : movieService.getByStatus(status));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getPaged(@RequestParam(defaultValue = "1") int pageNumber,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) String status) {
        return ResponseEntity.ok(movieService.getPaged(pageNumber, pageSize, search, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return movieService.getById(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> create(@RequestBody CreateMovieRequest request) {
        return ResponseEntity.status(201).body(movieService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody CreateMovieRequest request) {
        return movieService.update(id, request).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return movieService.softDelete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> publish(@PathVariable Integer id, @RequestParam(defaultValue = "showing") String status) {
        return movieService.publish(id, status)
                ? ResponseEntity.ok(Map.of("message", "Đã xuất bản phim lên website."))
                : ResponseEntity.notFound().build();
    }
}
