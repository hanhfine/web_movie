package com.webphim.api.controller;

import com.webphim.api.dto.BookingDtos.BookingRequest;
import com.webphim.api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public Object createBooking(@Valid @RequestBody BookingRequest request) {
        return bookingService.bookSeats(request);
    }

    @GetMapping("/history")
    public Object history(Authentication authentication) {
        return bookingService.getBookingHistory((Integer) authentication.getPrincipal());
    }
}
