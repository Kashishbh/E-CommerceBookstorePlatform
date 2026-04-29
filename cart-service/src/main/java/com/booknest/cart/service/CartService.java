package com.booknest.cart.service;

import com.booknest.cart.dto.CartItemRequest;
import com.booknest.cart.dto.CartResponse;

import java.util.List;

public interface CartService {
    CartResponse getCartByUser(Long userId);
    CartResponse addItem(Long userId, CartItemRequest request);
    CartResponse removeItem(Long userId, Long itemId);
    CartResponse updateQuantity(Long userId, Long itemId, Integer quantity);
    CartResponse clearCart(Long userId);
    
    List<CartResponse> getAllCarts();
}
