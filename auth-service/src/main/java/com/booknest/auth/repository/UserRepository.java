package com.booknest.auth.repository;

import com.booknest.auth.entity.Role;
import com.booknest.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(Role role);
    void deleteByUserId(Long userId);
}
