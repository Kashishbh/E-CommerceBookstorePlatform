package com.booknest.order.client;

import com.booknest.order.client.dto.WalletTransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "WALLET-SERVICE")
public interface WalletClient {

    @PutMapping("/wallet/{walletId}/pay")
    Object payMoney(@PathVariable("walletId") Long walletId,
                    @RequestBody WalletTransactionRequest request);

    @PutMapping("/wallet/{walletId}/refund")
    Object refundMoney(@PathVariable("walletId") Long walletId,
                       @RequestBody WalletTransactionRequest request);
}
