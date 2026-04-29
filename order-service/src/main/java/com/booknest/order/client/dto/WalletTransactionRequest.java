package com.booknest.order.client.dto;

import java.math.BigDecimal;

public class WalletTransactionRequest {
    private BigDecimal amount;
    private Long orderId;
    private String remarks;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
