package com.booknest.notification.service.impl;

import com.booknest.notification.dto.NotificationRequest;
import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.entity.Notification;
import com.booknest.notification.exception.BadRequestException;
import com.booknest.notification.exception.ResourceNotFoundException;
import com.booknest.notification.repository.NotificationRepository;
import com.booknest.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        validateRequest(request);

        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setMessage(request.getMessage());
        notification.setIsRead(false);

        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = getNotificationEntity(notificationId);
        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    public String markAllRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsRead(userId, false);
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
        return "All notifications marked as read";
    }

    @Override
    public List<NotificationResponse> getByUser(Long userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Notification notification = getNotificationEntity(notificationId);
        notificationRepository.delete(notification);
    }

    @Override
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String sendEmailAlert(NotificationRequest request) {
        validateRequest(request);

        if (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank()) {
            return "Email skipped because recipient email is missing";
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            return "Email skipped because SMTP configuration is missing";
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(request.getRecipientEmail());
        message.setSubject("BookNest Notification: " + request.getType());
        message.setText(request.getMessage());
        mailSender.send(message);

        return "Email alert sent successfully to userId: " + request.getUserId();
    }

    private Notification getNotificationEntity(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
    }

    private void validateRequest(NotificationRequest request) {
        if (request.getUserId() == null) throw new BadRequestException("User id is required");
        if (request.getType() == null || request.getType().isBlank()) throw new BadRequestException("Notification type is required");
        if (request.getMessage() == null || request.getMessage().isBlank()) throw new BadRequestException("Notification message is required");
    }

    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setUserId(notification.getUserId());
        response.setType(notification.getType());
        response.setMessage(notification.getMessage());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
