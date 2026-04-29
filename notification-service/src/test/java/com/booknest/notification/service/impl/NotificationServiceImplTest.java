package com.booknest.notification.service.impl;

import com.booknest.notification.dto.NotificationRequest;
import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.entity.Notification;
import com.booknest.notification.exception.BadRequestException;
import com.booknest.notification.exception.ResourceNotFoundException;
import com.booknest.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void sendNotification_shouldSaveSuccessfully() {
        NotificationRequest request = buildNotificationRequest();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(1L);
            notification.setCreatedAt(LocalDateTime.now());
            return notification;
        });

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals(1L, response.getNotificationId());
        assertEquals(1L, response.getUserId());
        assertEquals("ORDER", response.getType());
        assertEquals("Order placed successfully", response.getMessage());
        assertFalse(response.getIsRead());
    }

    @Test
    void sendNotification_shouldThrowExceptionWhenUserIdMissing() {
        NotificationRequest request = buildNotificationRequest();
        request.setUserId(null);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> notificationService.sendNotification(request)
        );

        assertEquals("User id is required", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotification_shouldThrowExceptionWhenTypeMissing() {
        NotificationRequest request = buildNotificationRequest();
        request.setType("");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> notificationService.sendNotification(request)
        );

        assertEquals("Notification type is required", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotification_shouldThrowExceptionWhenMessageMissing() {
        NotificationRequest request = buildNotificationRequest();
        request.setMessage(" ");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> notificationService.sendNotification(request)
        );

        assertEquals("Notification message is required", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_shouldUpdateFlag() {
        Notification notification = buildNotification();
        notification.setNotificationId(1L);
        notification.setIsRead(false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(1L);

        assertTrue(response.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldThrowExceptionWhenNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L));
    }

    @Test
    void markAllRead_shouldUpdateAllUnread() {
        Notification n1 = buildNotification();
        n1.setNotificationId(1L);
        n1.setIsRead(false);

        Notification n2 = buildNotification();
        n2.setNotificationId(2L);
        n2.setIsRead(false);

        when(notificationRepository.findByUserIdAndIsRead(1L, false)).thenReturn(List.of(n1, n2));

        String response = notificationService.markAllRead(1L);

        assertEquals("All notifications marked as read", response);
        assertTrue(n1.getIsRead());
        assertTrue(n2.getIsRead());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void getByUser_shouldReturnNotifications() {
        Notification notification = buildNotification();
        notification.setNotificationId(1L);

        when(notificationRepository.findByUserId(1L)).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getByUser(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getNotificationId());
        assertEquals(1L, responses.get(0).getUserId());
    }

    @Test
    void getUnreadCount_shouldReturnCorrectCount() {
        when(notificationRepository.countByUserIdAndIsRead(1L, false)).thenReturn(3L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(3L, count);
    }

    @Test
    void deleteNotification_shouldDeleteSuccessfully() {
        Notification notification = buildNotification();
        notification.setNotificationId(1L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_shouldThrowExceptionWhenNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.deleteNotification(99L));
    }

    @Test
    void getAll_shouldReturnAllNotifications() {
        Notification notification = buildNotification();
        notification.setNotificationId(1L);

        when(notificationRepository.findAll()).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getAll();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getNotificationId());
    }

    @Test
    void sendEmailAlert_shouldReturnSuccessMessage() {
        NotificationRequest request = buildNotificationRequest();

        String response = notificationService.sendEmailAlert(request);

        assertEquals("Email alert sent successfully to userId: 1", response);
    }

    @Test
    void sendNotification_shouldCaptureSavedValues() {
        NotificationRequest request = buildNotificationRequest();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.sendNotification(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();
        assertEquals(1L, savedNotification.getUserId());
        assertEquals("ORDER", savedNotification.getType());
        assertEquals("Order placed successfully", savedNotification.getMessage());
        assertFalse(savedNotification.getIsRead());
    }

    private NotificationRequest buildNotificationRequest() {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setType("ORDER");
        request.setMessage("Order placed successfully");
        return request;
    }

    private Notification buildNotification() {
        Notification notification = new Notification();
        notification.setNotificationId(1L);
        notification.setUserId(1L);
        notification.setType("ORDER");
        notification.setMessage("Order placed successfully");
        notification.setIsRead(false);
        return notification;
    }
}
