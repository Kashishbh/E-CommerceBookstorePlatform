package com.booknest.auth.service;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.ChangePasswordRequest;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.dto.UpdateProfileRequest;
import com.booknest.auth.dto.UserResponse;
import com.booknest.auth.entity.AuthProvider;
import com.booknest.auth.entity.Role;
import com.booknest.auth.entity.User;
import com.booknest.auth.exception.ResourceNotFoundException;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUserSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Kashish");
        request.setEmail("kas@example.com");
        request.setPassword("Password@123");
        request.setMobile("9876543210");

        User savedUser = buildUser();
        savedUser.setEmail("kas@example.com");

        when(userRepository.existsByEmail("kas@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("kas@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Registration successful", response.getMessage());
        assertEquals("kas@example.com", response.getUser().getEmail());
        assertEquals("ROLE_CUSTOMER", response.getUser().getRole());
    }

    @Test
    void register_shouldThrowExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("kas@example.com");

        when(userRepository.existsByEmail("kas@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnTokenSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("kas@example.com");
        request.setPassword("Password@123");

        User user = buildUser();
        user.setEmail("kas@example.com");
        user.setPasswordHash("encoded-password");
        user.setActive(true);

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("kas@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
        assertEquals("kas@example.com", response.getUser().getEmail());
    }

    @Test
    void login_shouldThrowExceptionWhenPasswordInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("kas@example.com");
        request.setPassword("WrongPassword");

        User user = buildUser();
        user.setEmail("kas@example.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encoded-password")).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_shouldThrowExceptionWhenUserInactive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("kas@example.com");
        request.setPassword("Password@123");

        User user = buildUser();
        user.setEmail("kas@example.com");
        user.setPasswordHash("encoded-password");
        user.setActive(false);

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "encoded-password")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("User account is inactive", exception.getMessage());
    }

    @Test
    void getProfile_shouldReturnUserSuccessfully() {
        User user = buildUser();
        user.setEmail("kas@example.com");

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.getProfile("kas@example.com");

        assertEquals("kas@example.com", response.getEmail());
        assertEquals("ROLE_ADMIN", response.getRole());
    }

    @Test
    void getProfile_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getProfile("missing@example.com"));
    }

    @Test
    void updateProfile_shouldUpdateUserSuccessfully() {
        User user = buildUser();
        user.setEmail("kas@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Kashish");
        request.setMobile("9999999999");

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.updateProfile("kas@example.com", request);

        assertEquals("Updated Kashish", response.getFullName());
        assertEquals("9999999999", response.getMobile());
    }

    @Test
    void changePassword_shouldChangePasswordSuccessfully() {
        User user = buildUser();
        user.setEmail("kas@example.com");
        user.setPasswordHash("old-encoded-password");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass@123");
        request.setNewPassword("NewPass@123");

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old-encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String response = authService.changePassword("kas@example.com", request);

        assertEquals("Password changed successfully", response);
        assertEquals("new-encoded-password", user.getPasswordHash());
    }

    @Test
    void changePassword_shouldThrowExceptionWhenOldPasswordIncorrect() {
        User user = buildUser();
        user.setEmail("kas@example.com");
        user.setPasswordHash("old-encoded-password");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WrongOldPass");
        request.setNewPassword("NewPass@123");

        when(userRepository.findByEmail("kas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOldPass", "old-encoded-password")).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.changePassword("kas@example.com", request)
        );

        assertEquals("Old password is incorrect", exception.getMessage());
    }

    @Test
    void suspendUser_shouldDeactivateUser() {
        User user = buildUser();
        user.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.suspendUser(1L);

        assertFalse(response.isActive());
    }

    @Test
    void activateUser_shouldActivateUser() {
        User user = buildUser();
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.activateUser(1L);

        assertTrue(response.isActive());
    }

    @Test
    void deleteUser_shouldDeleteUserSuccessfully() {
        User user = buildUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String response = authService.deleteUser(1L);

        assertEquals("User deleted successfully", response);
        verify(userRepository).delete(user);
    }

    @Test
    void getAllUsers_shouldReturnMappedUsers() {
        User user1 = buildUser();
        user1.setEmail("user1@example.com");

        User user2 = buildUser();
        user2.setUserId(2L);
        user2.setEmail("user2@example.com");
        user2.setRole(Role.ROLE_CUSTOMER);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> responses = authService.getAllUsers();

        assertEquals(2, responses.size());
        assertEquals("user1@example.com", responses.get(0).getEmail());
        assertEquals("user2@example.com", responses.get(1).getEmail());
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("Kashish");
        user.setEmail("admin@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(Role.ROLE_ADMIN);
        user.setProvider(AuthProvider.LOCAL);
        user.setMobile("9876543210");
        user.setActive(true);
        return user;
    }
}
