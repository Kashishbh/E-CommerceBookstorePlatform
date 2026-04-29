package com.booknest.wallet.service;

import com.booknest.wallet.dto.RazorpayOrderRequest;
import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.booknest.wallet.dto.RazorpayVerifyRequest;
import com.booknest.wallet.dto.WalletResponse;

public interface RazorpayService {
    RazorpayOrderResponse createTopupOrder(Long walletId, RazorpayOrderRequest request);
    WalletResponse verifyAndTopup(Long walletId, RazorpayVerifyRequest request);
}
