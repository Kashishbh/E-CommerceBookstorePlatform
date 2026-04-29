package com.booknest.wallet.repository;

import com.booknest.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByWalletId(Long walletId);
    Optional<Wallet> findByUserId(Long userId);
    void deleteByWalletId(Long walletId);
}
