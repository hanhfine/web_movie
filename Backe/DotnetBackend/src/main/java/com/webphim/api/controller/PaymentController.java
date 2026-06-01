package com.webphim.api.controller;

import com.webphim.api.dto.PaymentDtos.SePayWebhookRequest;
import com.webphim.api.service.PaymentService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private final String sepaySecret;

    public PaymentController(PaymentService paymentService, @Value("${webphim.sepay-webhook-secret:}") String sepaySecret) {
        this.paymentService = paymentService;
        this.sepaySecret = sepaySecret;
    }

    @GetMapping("/sepay-webhook")
    public Object verifyWebhook() {
        return Map.of("success", true);
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleWebhook(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody SePayWebhookRequest request) {
        if (sepaySecret != null && !sepaySecret.isBlank()
                && (authorization == null || !authorization.contains(sepaySecret))) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Lỗi xác thực Token"));
        }
        paymentService.processWebhook(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/status/{orderCode}")
    public Object checkStatus(@PathVariable String orderCode) {
        return paymentService.getBookingStatus(orderCode);
    }
}
