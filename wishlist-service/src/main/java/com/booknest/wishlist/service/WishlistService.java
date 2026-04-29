package com.booknest.wishlist.service;

import com.booknest.wishlist.dto.WishlistItemRequest;
import com.booknest.wishlist.dto.WishlistResponse;

import java.util.List;

public interface WishlistService {
    WishlistResponse getWishlistByUser(Long userId);
    WishlistResponse addBook(Long userId, WishlistItemRequest request);
    WishlistResponse removeBook(Long userId, Long bookId);
    WishlistResponse clearWishlist(Long userId);
    String moveToCart(Long userId, Long bookId);
    List<WishlistResponse> getAllWishlists();
}
