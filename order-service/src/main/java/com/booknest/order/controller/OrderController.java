package com.booknest.order.controller;

import com.booknest.order.dto.*;
import com.booknest.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin("*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getOrderByUserId(@PathVariable Long userId) {
        return orderService.getOrderByUserId(userId);
    }

    @PostMapping("/address")
    public AddressResponse storeAddress(@RequestBody AddressRequest request) {
        return orderService.storeAddress(request);
    }

    @PostMapping("/place")
    public OrderResponse placeOrder(@RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }

    @PostMapping("/online")
    public OrderResponse onlinePayment(@RequestBody OrderRequest request) {
        return orderService.onlinePayment(request);
    }

    @PutMapping("/{orderId}/status")
    public OrderResponse changeStatus(@PathVariable Long orderId,
                                      @RequestBody OrderStatusUpdateRequest request) {
        return orderService.changeStatus(orderId, request.getOrderStatus());
    }

    @DeleteMapping("/{orderId}")
    public String deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return "Order deleted successfully";
    }
    
    @GetMapping("/user/{userId}/product/{productId}/purchased")
    public boolean hasUserPurchasedBook(@PathVariable Long userId, @PathVariable Long productId) {
        return orderService.hasUserPurchasedBook(userId, productId);
    }

}
