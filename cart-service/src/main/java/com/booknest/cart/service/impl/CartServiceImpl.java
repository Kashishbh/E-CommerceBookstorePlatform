package com.booknest.cart.service.impl;

import com.booknest.cart.dto.CartItemRequest;
import com.booknest.cart.dto.CartItemResponse;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.entity.Cart;
import com.booknest.cart.entity.CartItem;
import com.booknest.cart.exception.BadRequestException;
import com.booknest.cart.exception.ResourceNotFoundException;
import com.booknest.cart.repository.CartRepository;
import com.booknest.cart.service.CartService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    public CartServiceImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public CartResponse getCartByUser(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setTotalPrice(BigDecimal.ZERO);
                    return cartRepository.save(newCart);
                });

        return mapToResponse(cart);
    }

    @Override
    public CartResponse addItem(Long userId, CartItemRequest request) {
        validateCartItemRequest(request);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setTotalPrice(BigDecimal.ZERO);
                    return cartRepository.save(newCart);
                });

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getBookId().equals(request.getBookId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setBookId(request.getBookId());
            item.setBookTitle(request.getBookTitle());
            item.setPrice(request.getPrice());
            item.setQuantity(request.getQuantity());
            item.setCart(cart);
            cart.getItems().add(item);
        }

        recalculateTotal(cart);
        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getCartEntityByUserId(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getItemId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        recalculateTotal(cart);
        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse updateQuantity(Long userId, Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Cart cart = getCartEntityByUserId(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItem.setQuantity(quantity);
        recalculateTotal(cart);

        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse clearCart(Long userId) {
        Cart cart = getCartEntityByUserId(userId);
        cart.getItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    public List<CartResponse> getAllCarts() {
        return cartRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Cart getCartEntityByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for userId: " + userId));
    }

    private void validateCartItemRequest(CartItemRequest request) {
        if (request.getBookId() == null) {
            throw new BadRequestException("Book id is required");
        }
        if (request.getBookTitle() == null || request.getBookTitle().isBlank()) {
            throw new BadRequestException("Book title is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
    }

    private void recalculateTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
    }

    private CartResponse mapToResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getCartId());
        response.setUserId(cart.getUserId());
        response.setTotalPrice(cart.getTotalPrice());

        List<CartItemResponse> items = cart.getItems().stream().map(item -> {
            CartItemResponse itemResponse = new CartItemResponse();
            itemResponse.setItemId(item.getItemId());
            itemResponse.setBookId(item.getBookId());
            itemResponse.setBookTitle(item.getBookTitle());
            itemResponse.setPrice(item.getPrice());
            itemResponse.setQuantity(item.getQuantity());
            return itemResponse;
        }).collect(Collectors.toList());

        response.setItems(items);
        return response;
    }
}
