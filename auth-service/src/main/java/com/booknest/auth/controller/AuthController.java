package com.booknest.auth.controller;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.ChangePasswordRequest;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RefreshTokenRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.dto.UpdateProfileRequest;
import com.booknest.auth.dto.UserResponse;
import com.booknest.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getToken()));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getProfile(authentication.getName()));
    }

    @GetMapping("/internal/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                      @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(Authentication authentication,
                                                 @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authentication.getName(), request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok(authService.logout());
    }

    @GetMapping("/oauth/success")
    public ResponseEntity<String> oauthSuccess(@RequestParam String token) {
        return ResponseEntity.ok("Google OAuth login successful. JWT Token: " + token);
    }
    
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/users/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.suspendUser(userId));
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/users/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.activateUser(userId));
    }
    
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.deleteUser(userId));
    }
}
