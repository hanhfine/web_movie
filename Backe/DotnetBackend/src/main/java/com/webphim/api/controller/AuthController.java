package com.webphim.api.controller;

import com.webphim.api.dto.AuthDtos.*;
import com.webphim.api.service.AuthService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .<ResponseEntity<?>>map(token -> ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie(token.accessToken(), Duration.ofDays(7)).toString())
                        .body(token))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Email hoặc mật khẩu không đúng")));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        var token = authService.register(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie(token.accessToken(), Duration.ofDays(7)).toString())
                .body(token);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        var userId = (Integer) authentication.getPrincipal();
        return authService.getMe(userId).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/update-profile")
    public ResponseEntity<Map<String, String>> updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        authService.updateProfile((Integer) authentication.getPrincipal(), request);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie("", Duration.ZERO).toString())
                .body(Map.of("message", "Đăng xuất thành công"));
    }

    private static ResponseCookie jwtCookie(String value, Duration maxAge) {
        return ResponseCookie.from("jwt", value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
