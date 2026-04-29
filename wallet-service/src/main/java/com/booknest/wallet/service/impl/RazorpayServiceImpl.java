package com.booknest.wallet.service.impl;

import com.booknest.wallet.dto.RazorpayOrderRequest;
import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.booknest.wallet.dto.RazorpayVerifyRequest;
import com.booknest.wallet.dto.WalletResponse;
import com.booknest.wallet.dto.WalletTransactionRequest;
import com.booknest.wallet.exception.BadRequestException;
import com.booknest.wallet.service.RazorpayService;
import com.booknest.wallet.service.WalletService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RazorpayServiceImpl implements RazorpayService {

    private final RazorpayClient razorpayClient;
    private final WalletService walletService;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public RazorpayServiceImpl(RazorpayClient razorpayClient, WalletService walletService) {
        this.razorpayClient = razorpayClient;
        this.walletService = walletService;
    }

    @Override
    public RazorpayOrderResponse createTopupOrder(Long walletId, RazorpayOrderRequest request) {
        try {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Amount must be greater than 0");
            }

            JSONObject options = new JSONObject();
            options.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
            options.put("currency", request.getCurrency() == null ? "INR" : request.getCurrency());
            options.put("receipt", request.getReceipt() == null ? "wallet_topup_" + walletId : request.getReceipt());

            Order order = razorpayClient.orders.create(options);

            RazorpayOrderResponse response = new RazorpayOrderResponse();
            response.setKeyId(keyId);
            response.setRazorpayOrderId(order.get("id").toString());
            response.setCurrency(order.get("currency").toString());

            Number amountValue = (Number) order.get("amount");
            response.setAmount(amountValue.longValue());

            return response;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to create Razorpay order: " + ex.getMessage());
        }
    }

    @Override
    public WalletResponse verifyAndTopup(Long walletId, RazorpayVerifyRequest request) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean valid = Utils.verifyPaymentSignature(options, keySecret);
            if (!valid) {
                throw new BadRequestException("Invalid Razorpay payment signature");
            }

            WalletTransactionRequest walletRequest = new WalletTransactionRequest();
            walletRequest.setAmount(request.getAmount());
            walletRequest.setOrderId(null);
            walletRequest.setRemarks(
                    request.getRemarks() != null ? request.getRemarks() : "Wallet top-up via Razorpay"
            );

            return walletService.addMoney(walletId, walletRequest);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to verify Razorpay payment: " + ex.getMessage());
        }
    }
}
