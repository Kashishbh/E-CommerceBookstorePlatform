package com.booknest.wallet.service;

import com.booknest.wallet.dto.CreateWalletRequest;
import com.booknest.wallet.dto.WalletResponse;
import com.booknest.wallet.dto.WalletTransactionRequest;
import com.booknest.wallet.dto.StatementResponse;

import java.util.List;

public interface WalletService {
    WalletResponse createWallet(CreateWalletRequest request);
    WalletResponse getWalletById(Long walletId);
    WalletResponse getWalletByUserId(Long userId);
    WalletResponse addMoney(Long walletId, WalletTransactionRequest request);
    WalletResponse payMoney(Long walletId, WalletTransactionRequest request);
    WalletResponse refundMoney(Long walletId, WalletTransactionRequest request);
    List<StatementResponse> getStatements(Long walletId);
    List<StatementResponse> getStatementsByOrderId(Long walletId, Long orderId);
}
