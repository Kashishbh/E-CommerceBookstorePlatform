package com.booknest.wallet.service.impl;

import com.booknest.wallet.dto.CreateWalletRequest;
import com.booknest.wallet.dto.NotificationEvent;
import com.booknest.wallet.dto.StatementResponse;
import com.booknest.wallet.dto.WalletResponse;
import com.booknest.wallet.dto.WalletTransactionRequest;
import com.booknest.wallet.entity.Statement;
import com.booknest.wallet.entity.Wallet;
import com.booknest.wallet.exception.BadRequestException;
import com.booknest.wallet.exception.ResourceNotFoundException;
import com.booknest.wallet.producer.NotificationEventProducer;
import com.booknest.wallet.repository.StatementRepository;
import com.booknest.wallet.repository.WalletRepository;
import com.booknest.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final StatementRepository statementRepository;
    private final NotificationEventProducer notificationEventProducer;

    public WalletServiceImpl(WalletRepository walletRepository,
                             StatementRepository statementRepository,
                             NotificationEventProducer notificationEventProducer) {
        this.walletRepository = walletRepository;
        this.statementRepository = statementRepository;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    public WalletResponse createWallet(CreateWalletRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("User id is required");
        }

        if (walletRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new BadRequestException("Wallet already exists for user id: " + request.getUserId());
        }

        Wallet wallet = new Wallet();
        wallet.setUserId(request.getUserId());
        wallet.setCurrentBalance(BigDecimal.ZERO);

        Wallet savedWallet = walletRepository.save(wallet);
        return mapToWalletResponse(savedWallet);
    }

    @Override
    public WalletResponse getWalletById(Long walletId) {
        return mapToWalletResponse(getWalletEntity(walletId));
    }

    @Override
    public WalletResponse getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user id: " + userId));
        return mapToWalletResponse(wallet);
    }

    @Override
    public WalletResponse addMoney(Long walletId, WalletTransactionRequest request) {
        validateAmount(request);

        Wallet wallet = getWalletEntity(walletId);
        wallet.setCurrentBalance(wallet.getCurrentBalance().add(request.getAmount()));

        Wallet updatedWallet = walletRepository.save(wallet);

        Statement statement = new Statement();
        statement.setWallet(updatedWallet);
        statement.setAmount(request.getAmount());
        statement.setTransactionType("DEPOSIT");
        statement.setOrderId(request.getOrderId());
        statement.setTransactionRemarks(request.getRemarks());
        statementRepository.save(statement);

        sendNotification(
                updatedWallet.getUserId(),
                "WALLET_TOPUP_SUCCESS",
                "Wallet topped up successfully with amount " + request.getAmount()
        );

        return mapToWalletResponse(updatedWallet);
    }

    @Override
    public WalletResponse payMoney(Long walletId, WalletTransactionRequest request) {
        validateAmount(request);

        Wallet wallet = getWalletEntity(walletId);

        if (wallet.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }

        wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(request.getAmount()));
        Wallet updatedWallet = walletRepository.save(wallet);

        Statement statement = new Statement();
        statement.setWallet(updatedWallet);
        statement.setAmount(request.getAmount());
        statement.setTransactionType("WITHDRAW");
        statement.setOrderId(request.getOrderId());
        statement.setTransactionRemarks(request.getRemarks());
        statementRepository.save(statement);

        sendNotification(
                updatedWallet.getUserId(),
                "PAYMENT_SUCCESS",
                "Payment successful for amount " + request.getAmount()
        );

        return mapToWalletResponse(updatedWallet);
    }

    @Override
    public WalletResponse refundMoney(Long walletId, WalletTransactionRequest request) {
        validateAmount(request);

        Wallet wallet = getWalletEntity(walletId);
        wallet.setCurrentBalance(wallet.getCurrentBalance().add(request.getAmount()));

        Wallet updatedWallet = walletRepository.save(wallet);

        Statement statement = new Statement();
        statement.setWallet(updatedWallet);
        statement.setAmount(request.getAmount());
        statement.setTransactionType("REFUND");
        statement.setOrderId(request.getOrderId());
        statement.setTransactionRemarks(request.getRemarks());
        statementRepository.save(statement);

        sendNotification(
                updatedWallet.getUserId(),
                "WALLET_REFUND_SUCCESS",
                "Refund successful for amount " + request.getAmount()
                        + (request.getOrderId() != null ? " against order " + request.getOrderId() : "")
        );

        return mapToWalletResponse(updatedWallet);
    }

    @Override
    public List<StatementResponse> getStatements(Long walletId) {
        getWalletEntity(walletId);

        return statementRepository.findByWalletWalletId(walletId)
                .stream()
                .map(this::mapToStatementResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StatementResponse> getStatementsByOrderId(Long walletId, Long orderId) {
        getWalletEntity(walletId);

        return statementRepository.findByWalletWalletIdAndOrderId(walletId, orderId)
                .stream()
                .map(this::mapToStatementResponse)
                .collect(Collectors.toList());
    }

    private void validateAmount(WalletTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }
    }

    private Wallet getWalletEntity(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
    }

    private void sendNotification(Long userId, String type, String message) {
        NotificationEvent event = new NotificationEvent();
        event.setUserId(userId);
        event.setType(type);
        event.setMessage(message);

        notificationEventProducer.publish(event);
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setWalletId(wallet.getWalletId());
        response.setUserId(wallet.getUserId());
        response.setBalance(wallet.getCurrentBalance());
        return response;
    }

    private StatementResponse mapToStatementResponse(Statement statement) {
        StatementResponse response = new StatementResponse();
        response.setStatementId(statement.getStatementId());
        response.setAmount(statement.getAmount());
        response.setType(statement.getTransactionType());
        response.setOrderId(statement.getOrderId());
        response.setRemarks(statement.getTransactionRemarks());
        response.setTransactionDate(statement.getDateTime());
        return response;
    }
}
