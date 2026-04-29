package com.booknest.order.service.impl;

import com.booknest.order.client.CartClient;
import com.booknest.order.client.CatalogClient;
import com.booknest.order.client.WalletClient;
import com.booknest.order.client.dto.BookResponse;
import com.booknest.order.dto.AddressRequest;
import com.booknest.order.dto.OrderRequest;
import com.booknest.order.dto.OrderResponse;
import com.booknest.order.entity.Address;
import com.booknest.order.entity.Order;
import com.booknest.order.exception.BadRequestException;
import com.booknest.order.exception.ResourceNotFoundException;
import com.booknest.order.producer.NotificationEventProducer;
import com.booknest.order.repository.AddressRepository;
import com.booknest.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private WalletClient walletClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void placeOrder_shouldPlaceCodOrderSuccessfully() {
        OrderRequest request = buildOrderRequest();
        Address address = buildAddress();
        BookResponse book = buildBookResponse(10);

        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(catalogClient.getBookById(1L)).thenReturn(book);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(11L);
            return order;
        });

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(11L, response.getOrderId());
        assertEquals("COD", response.getModeOfPayment());
        assertEquals("PLACED", response.getOrderStatus());
        assertEquals("Atomic Habits", response.getProductName());

        verify(catalogClient).updateStock(eq(1L), any());
        verify(notificationEventProducer).publish(any());
        verify(walletClient, never()).payMoney(anyLong(), any());
        verify(cartClient, never()).clearCart(anyLong());
    }

    @Test
    void placeOrder_shouldThrowExceptionWhenStockInsufficient() {
        OrderRequest request = buildOrderRequest();
        request.setQuantity(20);

        when(addressRepository.findById(1L)).thenReturn(Optional.of(buildAddress()));
        when(catalogClient.getBookById(1L)).thenReturn(buildBookResponse(5));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.placeOrder(request)
        );

        assertEquals("Requested quantity is not available in stock", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void onlinePayment_shouldPlaceOnlineOrderSuccessfully() {
        OrderRequest request = buildOrderRequest();
        request.setWalletId(1L);

        Address address = buildAddress();
        BookResponse book = buildBookResponse(8);

        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(catalogClient.getBookById(1L)).thenReturn(book);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(15L);
            return order;
        });

        OrderResponse response = orderService.onlinePayment(request);

        assertNotNull(response);
        assertEquals(15L, response.getOrderId());
        assertEquals("ONLINE", response.getModeOfPayment());
        assertEquals("CONFIRMED", response.getOrderStatus());
        assertEquals(1L, response.getWalletId());

        verify(walletClient).payMoney(eq(1L), any());
        verify(catalogClient).updateStock(eq(1L), any());
        verify(cartClient).clearCart(1L);
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void onlinePayment_shouldThrowExceptionWhenWalletIdMissing() {
        OrderRequest request = buildOrderRequest();
        request.setWalletId(null);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.onlinePayment(request)
        );

        assertEquals("Wallet id is required for online payment", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeStatus_shouldUpdateStatusSuccessfully() {
        Order order = buildSavedOrder();
        order.setOrderStatus("CONFIRMED");

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.changeStatus(10L, "DISPATCHED");

        assertEquals("DISPATCHED", response.getOrderStatus());
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void changeStatus_shouldThrowExceptionForInvalidStatus() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.changeStatus(10L, "RANDOM_STATUS")
        );

        assertEquals("Invalid order status", exception.getMessage());
        verify(orderRepository, never()).findById(anyLong());
    }


    @Test
    void changeStatus_shouldThrowExceptionWhenFinalizedOrderUpdated() {
        Order order = buildSavedOrder();
        order.setOrderStatus("DELIVERED");

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.changeStatus(10L, "CANCELLED")
        );

        assertEquals("Finalized orders cannot be updated", exception.getMessage());
    }

    @Test
    void changeStatus_shouldTriggerRefundWhenCancelledOnlineOrder() {
        Order order = buildSavedOrder();
        order.setModeOfPayment("ONLINE");
        order.setOrderStatus("CONFIRMED");
        order.setWalletId(1L);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.changeStatus(10L, "CANCELLED");

        assertEquals("CANCELLED", response.getOrderStatus());
        verify(walletClient).refundMoney(eq(1L), any());
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void changeStatus_shouldThrowExceptionWhenWalletIdMissingForRefund() {
        Order order = buildSavedOrder();
        order.setModeOfPayment("ONLINE");
        order.setOrderStatus("CONFIRMED");
        order.setWalletId(null);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.changeStatus(10L, "CANCELLED")
        );

        assertEquals("Wallet id not found for refund", exception.getMessage());
        verify(walletClient, never()).refundMoney(anyLong(), any());
    }

    @Test
    void getOrderById_shouldReturnOrderSuccessfully() {
        Order order = buildSavedOrder();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(10L);

        assertEquals(10L, response.getOrderId());
        assertEquals("Atomic Habits", response.getProductName());
        assertEquals(1L, response.getWalletId());
    }

    @Test
    void getOrderById_shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    void hasUserPurchasedBook_shouldReturnTrueWhenPurchased() {
        Order order = buildSavedOrder();
        order.setProductId(1L);
        order.setOrderStatus("DELIVERED");

        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));

        boolean result = orderService.hasUserPurchasedBook(1L, 1L);

        assertTrue(result);
    }

    @Test
    void hasUserPurchasedBook_shouldReturnFalseWhenOnlyCancelledOrderExists() {
        Order order = buildSavedOrder();
        order.setProductId(1L);
        order.setOrderStatus("CANCELLED");

        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));

        boolean result = orderService.hasUserPurchasedBook(1L, 1L);

        assertFalse(result);
    }

    @Test
    void storeAddress_shouldSaveAddressSuccessfully() {
        AddressRequest request = new AddressRequest();
        request.setCustomerId(1L);
        request.setFullName("Kashish");
        request.setMobileNumber("9876543210");
        request.setFlatNumber("A-12");
        request.setCity("Delhi");
        request.setPincode("110001");
        request.setState("Delhi");

        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setAddressId(21L);
            return address;
        });

        var response = orderService.storeAddress(request);

        assertEquals(21L, response.getAddressId());
        assertEquals(1L, response.getCustomerId());
        assertEquals("Kashish", response.getFullName());
    }

    @Test
    void placeOrder_shouldCaptureSavedOrderValuesCorrectly() {
        OrderRequest request = buildOrderRequest();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(buildAddress()));
        when(catalogClient.getBookById(1L)).thenReturn(buildBookResponse(10));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(30L);
            return order;
        });

        orderService.placeOrder(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();
        assertEquals("COD", savedOrder.getModeOfPayment());
        assertEquals("PLACED", savedOrder.getOrderStatus());
        assertEquals("Atomic Habits", savedOrder.getProductName());
    }

    private OrderRequest buildOrderRequest() {
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setAmountPaid(new BigDecimal("499"));
        request.setQuantity(1);
        request.setProductId(1L);
        request.setAddressId(1L);
        request.setWalletId(1L);
        return request;
    }

    private Address buildAddress() {
        Address address = new Address();
        address.setAddressId(1L);
        address.setCustomerId(1L);
        address.setFullName("Kashish");
        address.setMobileNumber("9876543210");
        address.setFlatNumber("A-12");
        address.setCity("Delhi");
        address.setPincode("110001");
        address.setState("Delhi");
        return address;
    }

    private BookResponse buildBookResponse(int stock) {
        BookResponse book = new BookResponse();
        book.setBookId(1L);
        book.setTitle("Atomic Habits");
        book.setPrice(new BigDecimal("499"));
        book.setStock(stock);
        return book;
    }

    private Order buildSavedOrder() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setUserId(1L);
        order.setOrderDate(LocalDateTime.now());
        order.setAmountPaid(new BigDecimal("499"));
        order.setModeOfPayment("ONLINE");
        order.setOrderStatus("CONFIRMED");
        order.setQuantity(1);
        order.setProductId(1L);
        order.setProductName("Atomic Habits");
        order.setWalletId(1L);
        order.setAddress(buildAddress());
        return order;
    }
}
