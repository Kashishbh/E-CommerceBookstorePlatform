package com.booknest.wishlist.service.impl;

import com.booknest.wishlist.client.CartClient;
import com.booknest.wishlist.client.dto.CartItemRequest;
import com.booknest.wishlist.dto.WishlistItemRequest;
import com.booknest.wishlist.dto.WishlistResponse;
import com.booknest.wishlist.entity.Wishlist;
import com.booknest.wishlist.entity.WishlistItem;
import com.booknest.wishlist.exception.BadRequestException;
import com.booknest.wishlist.exception.ResourceNotFoundException;
import com.booknest.wishlist.repository.WishlistRepository;
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
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private CartClient cartClient;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Test
    void getWishlistByUser_shouldReturnExistingWishlist() {
        Wishlist wishlist = buildWishlist();

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));

        WishlistResponse response = wishlistService.getWishlistByUser(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
    }

    @Test
    void getWishlistByUser_shouldCreateWishlistWhenNotExists() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> {
            Wishlist wishlist = invocation.getArgument(0);
            wishlist.setWishlistId(1L);
            return wishlist;
        });

        WishlistResponse response = wishlistService.getWishlistByUser(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void addBook_shouldAddSuccessfully() {
        Wishlist wishlist = buildWishlist();
        wishlist.setItems(new ArrayList<>());

        WishlistItemRequest request = new WishlistItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setBookPrice(new BigDecimal("499"));

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishlistResponse response = wishlistService.addBook(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getItems().size());
        assertEquals("Atomic Habits", response.getItems().get(0).getBookTitle());
    }

    @Test
    void addBook_shouldCreateWishlistWhenNotExists() {
        WishlistItemRequest request = new WishlistItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setBookPrice(new BigDecimal("499"));

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> {
            Wishlist wishlist = invocation.getArgument(0);
            wishlist.setWishlistId(1L);
            return wishlist;
        });

        WishlistResponse response = wishlistService.addBook(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void addBook_shouldThrowExceptionWhenDuplicate() {
        Wishlist wishlist = buildWishlist();
        WishlistItem item = buildWishlistItem();
        wishlist.getItems().add(item);

        WishlistItemRequest request = new WishlistItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setBookPrice(new BigDecimal("499"));

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> wishlistService.addBook(1L, request)
        );

        assertEquals("Book already exists in wishlist", exception.getMessage());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void removeBook_shouldRemoveSuccessfully() {
        Wishlist wishlist = buildWishlist();
        WishlistItem item = buildWishlistItem();
        item.setBookId(1L);
        wishlist.getItems().add(item);

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishlistResponse response = wishlistService.removeBook(1L, 1L);

        assertEquals(0, response.getItems().size());
    }

    @Test
    void removeBook_shouldThrowExceptionWhenBookNotFound() {
        Wishlist wishlist = buildWishlist();
        wishlist.setItems(new ArrayList<>());

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.removeBook(1L, 99L));
    }

    @Test
    void clearWishlist_shouldClearSuccessfully() {
        Wishlist wishlist = buildWishlist();
        wishlist.getItems().add(buildWishlistItem());

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishlistResponse response = wishlistService.clearWishlist(1L);

        assertEquals(0, response.getItems().size());
    }

    @Test
    void clearWishlist_shouldThrowExceptionWhenNotFound() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.clearWishlist(1L));
    }

    @Test
    void moveToCart_shouldMoveSuccessfully() {
        Wishlist wishlist = buildWishlist();
        WishlistItem item = buildWishlistItem();
        item.setBookId(1L);
        wishlist.getItems().add(item);

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String response = wishlistService.moveToCart(1L, 1L);

        assertEquals("Book moved from wishlist to cart successfully", response);
        assertEquals(0, wishlist.getItems().size());

        ArgumentCaptor<CartItemRequest> captor = ArgumentCaptor.forClass(CartItemRequest.class);
        verify(cartClient).addItem(eq(1L), captor.capture());

        CartItemRequest sentRequest = captor.getValue();
        assertEquals(1L, sentRequest.getBookId());
        assertEquals("Atomic Habits", sentRequest.getBookTitle());
        assertEquals(new BigDecimal("499"), sentRequest.getPrice());
        assertEquals(1, sentRequest.getQuantity());
    }

    @Test
    void moveToCart_shouldThrowExceptionWhenBookMissing() {
        Wishlist wishlist = buildWishlist();
        wishlist.setItems(new ArrayList<>());

        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.moveToCart(1L, 99L));
        verify(cartClient, never()).addItem(anyLong(), any());
    }

    @Test
    void getAllWishlists_shouldReturnAllWishlists() {
        Wishlist wishlist = buildWishlist();

        when(wishlistRepository.findAll()).thenReturn(List.of(wishlist));

        List<WishlistResponse> responses = wishlistService.getAllWishlists();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getUserId());
    }

    @Test
    void addBook_shouldThrowExceptionWhenPriceInvalid() {
        WishlistItemRequest request = new WishlistItemRequest();
        request.setBookId(1L);
        request.setBookTitle("Atomic Habits");
        request.setBookPrice(BigDecimal.ZERO);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> wishlistService.addBook(1L, request)
        );

        assertEquals("Book price must be greater than 0", exception.getMessage());
    }

    private Wishlist buildWishlist() {
        Wishlist wishlist = new Wishlist();
        wishlist.setWishlistId(1L);
        wishlist.setUserId(1L);
        wishlist.setItems(new ArrayList<>());
        return wishlist;
    }

    private WishlistItem buildWishlistItem() {
        WishlistItem item = new WishlistItem();
        item.setItemId(10L);
        item.setBookId(1L);
        item.setBookTitle("Atomic Habits");
        item.setBookPrice(new BigDecimal("499"));
        return item;
    }
}
