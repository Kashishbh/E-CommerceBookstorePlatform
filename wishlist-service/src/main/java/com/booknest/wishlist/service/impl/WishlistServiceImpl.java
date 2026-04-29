package com.booknest.wishlist.service.impl;

import com.booknest.wishlist.dto.WishlistItemRequest;
import com.booknest.wishlist.dto.WishlistItemResponse;
import com.booknest.wishlist.dto.WishlistResponse;
import com.booknest.wishlist.entity.Wishlist;
import com.booknest.wishlist.entity.WishlistItem;
import com.booknest.wishlist.exception.BadRequestException;
import com.booknest.wishlist.exception.ResourceNotFoundException;
import com.booknest.wishlist.repository.WishlistRepository;
import com.booknest.wishlist.service.WishlistService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.booknest.wishlist.client.CartClient;
import com.booknest.wishlist.client.dto.CartItemRequest;

@Transactional
@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CartClient cartClient;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, CartClient cartClient) {
        this.wishlistRepository = wishlistRepository;
        this.cartClient = cartClient;
    }


    @Override
    public WishlistResponse getWishlistByUser(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUserId(userId);
                    return wishlistRepository.save(newWishlist);
                });

        return mapToResponse(wishlist);
    }

    @Override
    public WishlistResponse addBook(Long userId, WishlistItemRequest request) {
        validateRequest(request);

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUserId(userId);
                    return wishlistRepository.save(newWishlist);
                });

        boolean alreadyExists = wishlist.getItems().stream()
                .anyMatch(item -> item.getBookId().equals(request.getBookId()));

        if (alreadyExists) {
            throw new BadRequestException("Book already exists in wishlist");
        }

        WishlistItem item = new WishlistItem();
        item.setBookId(request.getBookId());
        item.setBookTitle(request.getBookTitle());
        item.setBookPrice(request.getBookPrice());
        item.setWishlist(wishlist);

        wishlist.getItems().add(item);

        return mapToResponse(wishlistRepository.save(wishlist));
    }

    @Override
    public WishlistResponse removeBook(Long userId, Long bookId) {
        Wishlist wishlist = getWishlistEntityByUserId(userId);

        boolean removed = wishlist.getItems().removeIf(item -> item.getBookId().equals(bookId));
        if (!removed) {
            throw new ResourceNotFoundException("Book not found in wishlist");
        }

        return mapToResponse(wishlistRepository.save(wishlist));
    }

    @Override
    public WishlistResponse clearWishlist(Long userId) {
        Wishlist wishlist = getWishlistEntityByUserId(userId);
        wishlist.getItems().clear();
        return mapToResponse(wishlistRepository.save(wishlist));
    }

    @Override
    public String moveToCart(Long userId, Long bookId) {
        Wishlist wishlist = getWishlistEntityByUserId(userId);

        WishlistItem item = wishlist.getItems().stream()
                .filter(wishlistItem -> wishlistItem.getBookId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Book not found in wishlist"));

        CartItemRequest cartItemRequest = new CartItemRequest();
        cartItemRequest.setBookId(item.getBookId());
        cartItemRequest.setBookTitle(item.getBookTitle());
        cartItemRequest.setPrice(item.getBookPrice());
        cartItemRequest.setQuantity(1);

        cartClient.addItem(userId, cartItemRequest);

        wishlist.getItems().remove(item);
        wishlistRepository.save(wishlist);

        return "Book moved from wishlist to cart successfully";
    }


    @Override
    public List<WishlistResponse> getAllWishlists() {
        return wishlistRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Wishlist getWishlistEntityByUserId(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found for userId: " + userId));
    }

    private void validateRequest(WishlistItemRequest request) {
        if (request.getBookId() == null) {
            throw new BadRequestException("Book id is required");
        }
        if (request.getBookTitle() == null || request.getBookTitle().isBlank()) {
            throw new BadRequestException("Book title is required");
        }
        if (request.getBookPrice() == null || request.getBookPrice().doubleValue() <= 0) {
            throw new BadRequestException("Book price must be greater than 0");
        }
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        WishlistResponse response = new WishlistResponse();
        response.setWishlistId(wishlist.getWishlistId());
        response.setUserId(wishlist.getUserId());
        response.setCreatedAt(wishlist.getCreatedAt());

        List<WishlistItemResponse> items = wishlist.getItems().stream().map(item -> {
            WishlistItemResponse itemResponse = new WishlistItemResponse();
            itemResponse.setItemId(item.getItemId());
            itemResponse.setBookId(item.getBookId());
            itemResponse.setBookTitle(item.getBookTitle());
            itemResponse.setBookPrice(item.getBookPrice());
            return itemResponse;
        }).collect(Collectors.toList());

        response.setItems(items);
        return response;
    }
}
