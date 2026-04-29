package com.booknest.auth.service;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.ChangePasswordRequest;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.dto.UpdateProfileRequest;
import com.booknest.auth.dto.UserResponse;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String oldToken);
    UserResponse getProfile(String email);
    UserResponse getUserById(Long userId);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    String changePassword(String email, ChangePasswordRequest request);
    String logout();

    List<UserResponse> getAllUsers();
    UserResponse suspendUser(Long userId);
    UserResponse activateUser(Long userId);
    String deleteUser(Long userId);
}
