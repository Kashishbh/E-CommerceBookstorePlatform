package com.booknest.wallet.controller;

import com.booknest.wallet.dto.CreateWalletRequest;
import com.booknest.wallet.dto.StatementResponse;
import com.booknest.wallet.dto.WalletResponse;
import com.booknest.wallet.dto.WalletTransactionRequest;
import com.booknest.wallet.service.WalletService;
import org.springframework.web.bind.annotation.*;
import com.booknest.wallet.dto.RazorpayOrderRequest;
import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.booknest.wallet.dto.RazorpayVerifyRequest;
import com.booknest.wallet.service.RazorpayService;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@CrossOrigin("*")
public class WalletController {

    private final WalletService walletService;
    private final RazorpayService razorpayService;


    public WalletController(WalletService walletService, RazorpayService razorpayService) {
        this.walletService = walletService;
        this.razorpayService = razorpayService;
    }


    @PostMapping
    public WalletResponse createWallet(@RequestBody CreateWalletRequest request) {
        return walletService.createWallet(request);
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWalletById(@PathVariable Long walletId) {
        return walletService.getWalletById(walletId);
    }

    @GetMapping("/user/{userId}")
    public WalletResponse getWalletByUserId(@PathVariable Long userId) {
        return walletService.getWalletByUserId(userId);
    }

    @PutMapping("/{walletId}/add-money")
    public WalletResponse addMoney(@PathVariable Long walletId,
                                   @RequestBody WalletTransactionRequest request) {
        return walletService.addMoney(walletId, request);
    }

    @PutMapping("/{walletId}/pay")
    public WalletResponse payMoney(@PathVariable Long walletId,
                                   @RequestBody WalletTransactionRequest request) {
        return walletService.payMoney(walletId, request);
    }

    @PutMapping("/{walletId}/refund")
    public WalletResponse refundMoney(@PathVariable Long walletId,
                                      @RequestBody WalletTransactionRequest request) {
        return walletService.refundMoney(walletId, request);
    }

    @GetMapping("/{walletId}/statements")
    public List<StatementResponse> getStatements(@PathVariable Long walletId) {
        return walletService.getStatements(walletId);
    }

    @GetMapping("/{walletId}/statements/order/{orderId}")
    public List<StatementResponse> getStatementsByOrderId(@PathVariable Long walletId,
                                                          @PathVariable Long orderId) {
        return walletService.getStatementsByOrderId(walletId, orderId);
    }
    @PostMapping("/{walletId}/topup/create-order")
    public RazorpayOrderResponse createTopupOrder(@PathVariable Long walletId,
                                                  @RequestBody RazorpayOrderRequest request) {
        return razorpayService.createTopupOrder(walletId, request);
    }

    @PostMapping("/{walletId}/topup/verify")
    public WalletResponse verifyTopup(@PathVariable Long walletId,
                                      @RequestBody RazorpayVerifyRequest request) {
        return razorpayService.verifyAndTopup(walletId, request);
    }

}
