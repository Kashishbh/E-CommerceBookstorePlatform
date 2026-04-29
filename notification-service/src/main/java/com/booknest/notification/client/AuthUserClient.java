package com.booknest.notification.client;

import com.booknest.notification.dto.AuthUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthUserClient {

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;

    public AuthUserClient(RestTemplateBuilder restTemplateBuilder,
                          @Value("${auth-service.base-url}") String authServiceBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.authServiceBaseUrl = authServiceBaseUrl;
    }

    public AuthUserResponse getUserById(Long userId) {
        return restTemplate.getForObject(
                authServiceBaseUrl + "/api/v1/auth/internal/users/" + userId,
                AuthUserResponse.class
        );
    }
}
