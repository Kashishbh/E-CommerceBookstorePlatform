package com.booknest.wishlist.controller;

import com.booknest.wishlist.dto.WishlistItemRequest;
import com.booknest.wishlist.dto.WishlistResponse;
import com.booknest.wishlist.service.WishlistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@CrossOrigin("*")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/{userId}")
    public WishlistResponse getWishlistByUser(@PathVariable Long userId) {
        return wishlistService.getWishlistByUser(userId);
    }

    @PostMapping("/{userId}/items")
    public WishlistResponse addBook(@PathVariable Long userId, @RequestBody WishlistItemRequest request) {
        return wishlistService.addBook(userId, request);
    }

    @DeleteMapping("/{userId}/items/{bookId}")
    public WishlistResponse removeBook(@PathVariable Long userId, @PathVariable Long bookId) {
        return wishlistService.removeBook(userId, bookId);
    }

    @DeleteMapping("/{userId}")
    public WishlistResponse clearWishlist(@PathVariable Long userId) {
        return wishlistService.clearWishlist(userId);
    }

    @PostMapping("/{userId}/move-to-cart/{bookId}")
    public String moveToCart(@PathVariable Long userId, @PathVariable Long bookId) {
        return wishlistService.moveToCart(userId, bookId);
    }

    @GetMapping
    public List<WishlistResponse> getAllWishlists() {
        return wishlistService.getAllWishlists();
    }
}
