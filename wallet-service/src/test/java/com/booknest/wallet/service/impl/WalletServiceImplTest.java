package com.booknest.wallet.service.impl;

import com.booknest.wallet.dto.CreateWalletRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWallet_shouldCreateWalletSuccessfully() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(1L);

        Wallet savedWallet = new Wallet();
        savedWallet.setWalletId(1L);
        savedWallet.setUserId(1L);
        savedWallet.setCurrentBalance(BigDecimal.ZERO);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        WalletResponse response = walletService.createWallet(request);

        assertNotNull(response);
        assertEquals(1L, response.getWalletId());
        assertEquals(1L, response.getUserId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_shouldThrowExceptionWhenWalletAlreadyExists() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(1L);

        Wallet existingWallet = new Wallet();
        existingWallet.setWalletId(1L);
        existingWallet.setUserId(1L);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(existingWallet));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> walletService.createWallet(request)
        );

        assertEquals("Wallet already exists for user id: 1", exception.getMessage());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getWalletById_shouldReturnWalletSuccessfully() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);
        wallet.setCurrentBalance(new BigDecimal("1000"));

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWalletById(1L);

        assertEquals(1L, response.getWalletId());
        assertEquals(1L, response.getUserId());
        assertEquals(new BigDecimal("1000"), response.getBalance());
    }

    @Test
    void getWalletById_shouldThrowExceptionWhenWalletNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.getWalletById(1L));
    }

    @Test
    void addMoney_shouldIncreaseBalanceAndPublishNotification() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);
        wallet.setCurrentBalance(new BigDecimal("500"));

        WalletTransactionRequest request = new WalletTransactionRequest();
        request.setAmount(new BigDecimal("1000"));
        request.setRemarks("Top up");
        request.setOrderId(null);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.addMoney(1L, request);

        assertEquals(new BigDecimal("1500"), response.getBalance());
        verify(statementRepository).save(any(Statement.class));
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void payMoney_shouldDeductBalanceAndPublishNotification() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);
        wallet.setCurrentBalance(new BigDecimal("1000"));

        WalletTransactionRequest request = new WalletTransactionRequest();
        request.setAmount(new BigDecimal("400"));
        request.setOrderId(10L);
        request.setRemarks("Payment");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.payMoney(1L, request);

        assertEquals(new BigDecimal("600"), response.getBalance());
        verify(statementRepository).save(any(Statement.class));
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void payMoney_shouldThrowExceptionWhenBalanceInsufficient() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);
        wallet.setCurrentBalance(new BigDecimal("100"));

        WalletTransactionRequest request = new WalletTransactionRequest();
        request.setAmount(new BigDecimal("400"));
        request.setOrderId(10L);
        request.setRemarks("Payment");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> walletService.payMoney(1L, request)
        );

        assertEquals("Insufficient wallet balance", exception.getMessage());
        verify(statementRepository, never()).save(any(Statement.class));
        verify(notificationEventProducer, never()).publish(any());
    }

    @Test
    void refundMoney_shouldIncreaseBalanceAndPublishNotification() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);
        wallet.setCurrentBalance(new BigDecimal("600"));

        WalletTransactionRequest request = new WalletTransactionRequest();
        request.setAmount(new BigDecimal("200"));
        request.setOrderId(10L);
        request.setRemarks("Refund");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.refundMoney(1L, request);

        assertEquals(new BigDecimal("800"), response.getBalance());
        verify(statementRepository).save(any(Statement.class));
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void getStatements_shouldReturnMappedStatements() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);

        Statement statement = new Statement();
        statement.setStatementId(1L);
        statement.setAmount(new BigDecimal("100"));
        statement.setTransactionType("DEPOSIT");
        statement.setOrderId(null);
        statement.setTransactionRemarks("Top up");
        statement.setDateTime(LocalDateTime.now());

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(statementRepository.findByWalletWalletId(1L)).thenReturn(List.of(statement));

        List<StatementResponse> responses = walletService.getStatements(1L);

        assertEquals(1, responses.size());
        assertEquals("DEPOSIT", responses.get(0).getType());
        assertEquals(new BigDecimal("100"), responses.get(0).getAmount());
    }

    @Test
    void getStatementsByOrderId_shouldReturnFilteredStatements() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);

        Statement statement = new Statement();
        statement.setStatementId(1L);
        statement.setAmount(new BigDecimal("400"));
        statement.setTransactionType("WITHDRAW");
        statement.setOrderId(11L);
        statement.setTransactionRemarks("Order payment");
        statement.setDateTime(LocalDateTime.now());

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(statementRepository.findByWalletWalletIdAndOrderId(1L, 11L))
                .thenReturn(List.of(statement));

        List<StatementResponse> responses = walletService.getStatementsByOrderId(1L, 11L);

        assertEquals(1, responses.size());
        assertEquals(11L, responses.get(0).getOrderId());
        assertEquals("WITHDRAW", responses.get(0).getType());
    }

    @Test
    void addMoney_shouldThrowExceptionWhenAmountInvalid() {
        WalletTransactionRequest request = new WalletTransactionRequest();
        request.setAmount(BigDecimal.ZERO);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> walletService.addMoney(1L, request)
        );

        assertEquals("Amount must be greater than 0", exception.getMessage());
    }

    @Test
    void addMoney_shouldSaveDepositStatementWithCorrectType() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(1L);
        wallet.setCurrentBalance(new BigDecimal("500"));

        WalletTransactionRequest request = new WalletTransactionRequest();
        request.setAmount(new BigDecimal("250"));
        request.setRemarks("Deposit test");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.addMoney(1L, request);

        ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
        verify(statementRepository).save(captor.capture());

        Statement savedStatement = captor.getValue();
        assertEquals("DEPOSIT", savedStatement.getTransactionType());
        assertEquals(new BigDecimal("250"), savedStatement.getAmount());
        assertEquals("Deposit test", savedStatement.getTransactionRemarks());
    }
}
