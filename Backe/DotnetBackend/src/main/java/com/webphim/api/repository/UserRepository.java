package com.webphim.api.repository;

import com.webphim.api.entity.User;
import com.webphim.api.entity.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmailOrUsername(String email, String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRoleAndCreatedAtGreaterThanEqual(UserRole role, LocalDateTime createdAt);
    List<User> findTop6ByRoleOrderByCreatedAtDesc(UserRole role);
}
