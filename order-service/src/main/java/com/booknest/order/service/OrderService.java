package com.booknest.order.service;

import com.booknest.order.dto.AddressRequest;
import com.booknest.order.dto.AddressResponse;
import com.booknest.order.dto.OrderRequest;
import com.booknest.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    List<OrderResponse> getAllOrders();
    OrderResponse placeOrder(OrderRequest request);
    OrderResponse onlinePayment(OrderRequest request);
    OrderResponse changeStatus(Long orderId, String status);
    void deleteOrder(Long orderId);
    List<OrderResponse> getOrderByUserId(Long userId);
    AddressResponse storeAddress(AddressRequest request);
    OrderResponse getOrderById(Long orderId);
    boolean hasUserPurchasedBook(Long userId, Long productId);
}
