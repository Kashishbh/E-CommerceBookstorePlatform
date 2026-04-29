package com.booknest.notification.service;

import com.booknest.notification.dto.NotificationRequest;
import com.booknest.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {
    NotificationResponse sendNotification(NotificationRequest request);
    NotificationResponse markAsRead(Long notificationId);
    String markAllRead(Long userId);
    List<NotificationResponse> getByUser(Long userId);
    long getUnreadCount(Long userId);
    void deleteNotification(Long notificationId);
    List<NotificationResponse> getAll();
    String sendEmailAlert(NotificationRequest request);
}
