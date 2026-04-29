package com.booknest.cart.service.impl;

import com.booknest.cart.dto.CartItemRequest;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.entity.Cart;
import com.booknest.cart.entity.CartItem;
import com.booknest.cart.exception.BadRequestException;
import com.booknest.cart.exception.ResourceNotFoundException;
import com.booknest.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void getCartByUser_shouldReturnExistingCart() {
        Cart cart = buildCart();
        cart.setItems(new ArrayList<>());
        cart.setTotalPrice(BigDecimal.ZERO);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCartByUser(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
    }

    @Test
    void getCartByUser_shouldCreateNewCartWhenNotExists() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setCartId(1L);
            return cart;
        });

        CartResponse response = cartService.getCartByUser(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_shouldAddItemToExistingCart() {
        Cart cart = buildCart();
        cart.setItems(new ArrayList<>());

        CartItemRequest request = new CartItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setPrice(new BigDecimal("499"));
        request.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(1L, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("998"), response.getTotalPrice());
    }

    @Test
    void addItem_shouldCreateCartWhenNotExists() {
        CartItemRequest request = new CartItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setPrice(new BigDecimal("499"));
        request.setQuantity(1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            if (cart.getCartId() == null) {
                cart.setCartId(1L);
            }
            return cart;
        });

        CartResponse response = cartService.addItem(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("499"), response.getTotalPrice());
    }

    @Test
    void addItem_shouldIncreaseQuantityWhenBookAlreadyExists() {
        Cart cart = buildCart();
        CartItem item = buildCartItem();
        item.setItemId(10L);
        item.setQuantity(1);
        cart.getItems().add(item);
        cart.setTotalPrice(new BigDecimal("499"));

        CartItemRequest request = new CartItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setPrice(new BigDecimal("499"));
        request.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(1L, request);

        assertEquals(1, response.getItems().size());
        assertEquals(3, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("1497"), response.getTotalPrice());
    }

    @Test
    void addItem_shouldThrowExceptionWhenQuantityInvalid() {
        CartItemRequest request = new CartItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setPrice(new BigDecimal("499"));
        request.setQuantity(0);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cartService.addItem(1L, request)
        );

        assertEquals("Quantity must be greater than 0", exception.getMessage());
    }

    @Test
    void removeItem_shouldRemoveSuccessfully() {
        Cart cart = buildCart();
        CartItem item = buildCartItem();
        item.setItemId(10L);
        cart.getItems().add(item);
        cart.setTotalPrice(new BigDecimal("499"));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeItem(1L, 10L);

        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
    }

    @Test
    void removeItem_shouldThrowExceptionWhenItemNotFound() {
        Cart cart = buildCart();
        cart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(ResourceNotFoundException.class, () -> cartService.removeItem(1L, 99L));
    }

    @Test
    void updateQuantity_shouldUpdateSuccessfully() {
        Cart cart = buildCart();
        CartItem item = buildCartItem();
        item.setItemId(10L);
        cart.getItems().add(item);
        cart.setTotalPrice(new BigDecimal("499"));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.updateQuantity(1L, 10L, 3);

        assertEquals(3, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("1497"), response.getTotalPrice());
    }

    @Test
    void updateQuantity_shouldThrowExceptionWhenQuantityInvalid() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cartService.updateQuantity(1L, 10L, 0)
        );

        assertEquals("Quantity must be greater than 0", exception.getMessage());
    }

    @Test
    void updateQuantity_shouldThrowExceptionWhenItemNotFound() {
        Cart cart = buildCart();
        cart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(ResourceNotFoundException.class, () -> cartService.updateQuantity(1L, 99L, 2));
    }

    @Test
    void clearCart_shouldClearSuccessfully() {
        Cart cart = buildCart();
        CartItem item = buildCartItem();
        cart.getItems().add(item);
        cart.setTotalPrice(new BigDecimal("499"));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.clearCart(1L);

        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
    }

    @Test
    void clearCart_shouldThrowExceptionWhenCartNotFound() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.clearCart(1L));
    }

    @Test
    void getAllCarts_shouldReturnAllCarts() {
        Cart cart = buildCart();
        cart.setItems(new ArrayList<>());

        when(cartRepository.findAll()).thenReturn(List.of(cart));

        List<CartResponse> responses = cartService.getAllCarts();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getUserId());
    }

    @Test
    void addItem_shouldCaptureSavedCartValues() {
        Cart cart = buildCart();
        cart.setItems(new ArrayList<>());

        CartItemRequest request = new CartItemRequest();
        request.setBookId(2L);
        request.setBookTitle("Deep Work");
        request.setPrice(new BigDecimal("300"));
        request.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.addItem(1L, request);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, atLeastOnce()).save(captor.capture());

        Cart savedCart = captor.getValue();
        assertEquals(new BigDecimal("600"), savedCart.getTotalPrice());
        assertEquals(1, savedCart.getItems().size());
    }

    private Cart buildCart() {
        Cart cart = new Cart();
        cart.setCartId(1L);
        cart.setUserId(1L);
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setItems(new ArrayList<>());
        return cart;
    }

    private CartItem buildCartItem() {
        CartItem item = new CartItem();
        item.setItemId(10L);
        item.setBookId(1L);
        item.setBookTitle("Atomic Habits");
        item.setPrice(new BigDecimal("499"));
        item.setQuantity(1);
        return item;
    }
}
