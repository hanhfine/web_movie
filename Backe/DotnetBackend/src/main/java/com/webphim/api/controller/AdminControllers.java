package com.webphim.api.controller;

import com.webphim.api.dto.BookingDtos.*;
import com.webphim.api.service.AdminBookingService;
import com.webphim.api.service.AdminReportService;
import com.webphim.api.service.AdminTicketService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

public final class AdminControllers {
    private AdminControllers() {}
}

@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasAnyRole('admin','staff')")
class AdminBookingController {
    private final AdminBookingService service;

    AdminBookingController(AdminBookingService service) {
        this.service = service;
    }

    @PostMapping
    Object bookAtCounter(@Valid @RequestBody AdminBookingRequest request) {
        return service.bookTicketsAtCounter(request);
    }
}

@RestController
@RequestMapping("/api/admin/tickets")
@PreAuthorize("hasAnyRole('admin','staff')")
class AdminTicketController {
    private final AdminTicketService service;

    AdminTicketController(AdminTicketService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    Object scan(@Valid @RequestBody TicketScanRequest request) {
        return service.scanTicket(request.orderCode());
    }
}

@RestController
@RequestMapping("/api/admin/invoices")
@PreAuthorize("hasAnyRole('admin','staff')")
class AdminInvoiceController {
    private final AdminReportService service;

    AdminInvoiceController(AdminReportService service) {
        this.service = service;
    }

    @GetMapping
    Object invoices(@RequestParam(required = false) String searchTerm,
                    @RequestParam(required = false) LocalDateTime fromDate,
                    @RequestParam(required = false) LocalDateTime toDate,
                    @RequestParam(required = false) BigDecimal minAmount,
                    @RequestParam(required = false) BigDecimal maxAmount) {
        return service.getPaidInvoices(fromDate, toDate, searchTerm, minAmount, maxAmount);
    }
}

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasAnyRole('admin','staff')")
class AdminReportController {
    private final AdminReportService service;

    AdminReportController(AdminReportService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    Object overview(@RequestParam(required = false) LocalDateTime from,
                    @RequestParam(required = false) LocalDateTime to) {
        return service.getOverview(from, to);
    }
}
