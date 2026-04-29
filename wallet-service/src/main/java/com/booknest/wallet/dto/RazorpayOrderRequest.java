package com.booknest.wallet.dto;

import java.math.BigDecimal;

public class RazorpayOrderRequest {

    private BigDecimal amount;
    private String currency = "INR";
    private String receipt;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
}
