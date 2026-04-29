package com.booknest.cart.controller;

import com.booknest.cart.dto.CartItemQuantityUpdateRequest;
import com.booknest.cart.dto.CartItemRequest;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@CrossOrigin("*")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public CartResponse getCartByUser(@PathVariable Long userId) {
        return cartService.getCartByUser(userId);
    }

    @PostMapping("/{userId}/items")
    public CartResponse addItem(@PathVariable Long userId, @RequestBody CartItemRequest request) {
        return cartService.addItem(userId, request);
    }

    @PutMapping("/{userId}/items/{itemId}")
    public CartResponse updateQuantity(@PathVariable Long userId,
                                       @PathVariable Long itemId,
                                       @RequestBody CartItemQuantityUpdateRequest request) {
        return cartService.updateQuantity(userId, itemId, request.getQuantity());
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long userId, @PathVariable Long itemId) {
        return cartService.removeItem(userId, itemId);
    }

    @DeleteMapping("/{userId}")
    public CartResponse clearCart(@PathVariable Long userId) {
        return cartService.clearCart(userId);
    }

    @GetMapping
    public List<CartResponse> getAllCarts() {
        return cartService.getAllCarts();
    }
}
