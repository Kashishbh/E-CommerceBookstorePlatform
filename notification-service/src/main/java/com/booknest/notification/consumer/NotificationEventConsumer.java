package com.booknest.notification.consumer;

import com.booknest.notification.client.AuthUserClient;
import com.booknest.notification.dto.AuthUserResponse;
import com.booknest.notification.dto.NotificationEvent;
import com.booknest.notification.dto.NotificationRequest;
import com.booknest.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final AuthUserClient authUserClient;

    public NotificationEventConsumer(NotificationService notificationService,
                                     AuthUserClient authUserClient) {
        this.notificationService = notificationService;
        this.authUserClient = authUserClient;
    }

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void consume(NotificationEvent event) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(event.getUserId());
        request.setType(event.getType());
        request.setMessage(event.getMessage());

        notificationService.sendNotification(request);

        try {
            AuthUserResponse user = authUserClient.getUserById(event.getUserId());
            request.setRecipientEmail(user != null ? user.getEmail() : null);
            request.setRecipientName(user != null ? user.getFullName() : null);
            notificationService.sendEmailAlert(request);
        } catch (Exception ignored) {
            // Keep RabbitMQ notification persistence stable even if email delivery is unavailable.
        }
    }
}
