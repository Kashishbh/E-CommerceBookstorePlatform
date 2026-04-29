package com.booknest.wallet.producer;

import com.booknest.wallet.dto.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public NotificationEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(NotificationEvent event) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
    }
}
