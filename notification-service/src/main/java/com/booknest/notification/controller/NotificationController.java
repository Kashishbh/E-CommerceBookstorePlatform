package com.booknest.notification.controller;

import com.booknest.notification.dto.NotificationRequest;
import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@CrossOrigin("*")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getAll() {
        return notificationService.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getByUser(@PathVariable Long userId) {
        return notificationService.getByUser(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public long getUnreadCount(@PathVariable Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PostMapping
    public NotificationResponse sendNotification(@RequestBody NotificationRequest request) {
        return notificationService.sendNotification(request);
    }

    @PostMapping("/email")
    public String sendEmailAlert(@RequestBody NotificationRequest request) {
        return notificationService.sendEmailAlert(request);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @PutMapping("/user/{userId}/read-all")
    public String markAllRead(@PathVariable Long userId) {
        return notificationService.markAllRead(userId);
    }

    @DeleteMapping("/{notificationId}")
    public String deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return "Notification deleted successfully";
    }
}
