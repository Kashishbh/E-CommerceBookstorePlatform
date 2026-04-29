package com.booknest.wallet.dto;

import java.io.Serializable;

public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String type;
    private String message;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
