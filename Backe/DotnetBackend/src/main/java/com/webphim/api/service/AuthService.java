package com.webphim.api.service;

import com.webphim.api.config.JwtService;
import com.webphim.api.dto.AuthDtos.*;
import com.webphim.api.entity.User;
import com.webphim.api.entity.UserRole;
import com.webphim.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final JwtService jwtService;

    public AuthService(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    public Optional<AuthResponse> login(LoginRequest request) {
        return users.findByEmailOrUsername(request.email(), request.email())
                .filter(user -> BCrypt.checkpw(request.password(), user.getPasswordHash()))
                .map(user -> new AuthResponse(jwtService.generateToken(user)));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (users.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        var user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(UserRole.customer);
        user.setCreatedAt(LocalDateTime.now());
        users.save(user);
        return new AuthResponse(jwtService.generateToken(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserInfoResponse> getMe(Integer userId) {
        return users.findById(userId)
                .map(u -> new UserInfoResponse(u.getUserId(), u.getUsername(), u.getEmail(),
                        u.getFullName(), u.getPhoneNumber(), u.getRole().name(), u.getPoints()));
    }

    @Transactional
    public void updateProfile(Integer userId, UpdateProfileRequest request) {
        var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
    }
}
