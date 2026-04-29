package com.booknest.wishlist.repository;

import com.booknest.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Optional<Wishlist> findByWishlistId(Long wishlistId);
    void deleteByUserId(Long userId);
}
