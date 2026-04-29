package com.booknest.order.service.impl;

import com.booknest.order.client.CartClient;
import com.booknest.order.client.CatalogClient;
import com.booknest.order.client.WalletClient;
import com.booknest.order.client.dto.BookResponse;
import com.booknest.order.client.dto.StockUpdateRequest;
import com.booknest.order.client.dto.WalletTransactionRequest;
import com.booknest.order.dto.AddressRequest;
import com.booknest.order.dto.AddressResponse;
import com.booknest.order.dto.NotificationEvent;
import com.booknest.order.dto.OrderRequest;
import com.booknest.order.dto.OrderResponse;
import com.booknest.order.entity.Address;
import com.booknest.order.entity.Order;
import com.booknest.order.exception.BadRequestException;
import com.booknest.order.exception.ResourceNotFoundException;
import com.booknest.order.producer.NotificationEventProducer;
import com.booknest.order.repository.AddressRepository;
import com.booknest.order.repository.OrderRepository;
import com.booknest.order.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("PLACED", "CONFIRMED", "DISPATCHED", "DELIVERED", "CANCELLED");

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CatalogClient catalogClient;
    private final WalletClient walletClient;
    private final CartClient cartClient;
    private final NotificationEventProducer notificationEventProducer;

    public OrderServiceImpl(OrderRepository orderRepository,
                            AddressRepository addressRepository,
                            CatalogClient catalogClient,
                            WalletClient walletClient,
                            CartClient cartClient,
                            NotificationEventProducer notificationEventProducer) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.catalogClient = catalogClient;
        this.walletClient = walletClient;
        this.cartClient = cartClient;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        validateOrderRequest(request);

        Address address = getAddress(request.getAddressId());
        BookResponse book = catalogClient.getBookById(request.getProductId());

        if (book.getStock() == null || book.getStock() < request.getQuantity()) {
            throw new BadRequestException("Requested quantity is not available in stock");
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmountPaid(request.getAmountPaid());
        order.setQuantity(request.getQuantity());
        order.setProductId(request.getProductId());
        order.setProductName(book.getTitle());
        order.setModeOfPayment("COD");
        order.setOrderStatus("PLACED");
        order.setAddress(address);

        Order savedOrder = orderRepository.save(order);

        StockUpdateRequest stockRequest = new StockUpdateRequest();
        stockRequest.setStock(book.getStock() - request.getQuantity());
        catalogClient.updateStock(request.getProductId(), stockRequest);

        cartClient.clearCart(request.getUserId());

        sendNotification(
                request.getUserId(),
                "ORDER",
                "Your order " + savedOrder.getOrderId() + " has been placed successfully."
        );

        return mapToOrderResponse(savedOrder);
    }

    @Override
    public OrderResponse onlinePayment(OrderRequest request) {
        validateOrderRequest(request);

        if (request.getWalletId() == null) {
            throw new BadRequestException("Wallet id is required for online payment");
        }

        Address address = getAddress(request.getAddressId());
        BookResponse book = catalogClient.getBookById(request.getProductId());

        if (book.getStock() == null || book.getStock() < request.getQuantity()) {
            throw new BadRequestException("Requested quantity is not available in stock");
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmountPaid(request.getAmountPaid());
        order.setQuantity(request.getQuantity());
        order.setProductId(request.getProductId());
        order.setProductName(book.getTitle());
        order.setModeOfPayment("ONLINE");
        order.setOrderStatus("CONFIRMED");
        order.setWalletId(request.getWalletId());
        order.setAddress(address);

        Order savedOrder = orderRepository.save(order);

        WalletTransactionRequest walletRequest = new WalletTransactionRequest();
        walletRequest.setAmount(request.getAmountPaid());
        walletRequest.setOrderId(savedOrder.getOrderId());
        walletRequest.setRemarks("Payment for order " + savedOrder.getOrderId());
        walletClient.payMoney(request.getWalletId(), walletRequest);

        StockUpdateRequest stockRequest = new StockUpdateRequest();
        stockRequest.setStock(book.getStock() - request.getQuantity());
        catalogClient.updateStock(request.getProductId(), stockRequest);

        cartClient.clearCart(request.getUserId());

        sendNotification(
                request.getUserId(),
                "PAYMENT_SUCCESS",
                "Payment successful and your order " + savedOrder.getOrderId() + " has been confirmed."
        );

        return mapToOrderResponse(savedOrder);
    }

    @Override
    public OrderResponse changeStatus(Long orderId, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Order status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BadRequestException("Invalid order status");
        }

        Order order = getOrderEntity(orderId);
        String previousStatus = order.getOrderStatus();

        if (normalizedStatus.equals(previousStatus)) {
            return mapToOrderResponse(order);
        }

        if ("DELIVERED".equals(previousStatus) || "CANCELLED".equals(previousStatus)) {
            throw new BadRequestException("Finalized orders cannot be updated");
        }

        order.setOrderStatus(normalizedStatus);
        Order updatedOrder = orderRepository.save(order);

        if ("CANCELLED".equals(normalizedStatus) && "ONLINE".equalsIgnoreCase(order.getModeOfPayment())) {
            processRefund(order);
        }

        sendNotification(
                order.getUserId(),
                "ORDER_STATUS",
                "Your order " + order.getOrderId() + " status has been updated to " + normalizedStatus
        );

        return mapToOrderResponse(updatedOrder);
    }

    @Override
    public void deleteOrder(Long orderId) {
        Order order = getOrderEntity(orderId);
        orderRepository.delete(order);
    }

    @Override
    public List<OrderResponse> getOrderByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse storeAddress(AddressRequest request) {
        if (request.getCustomerId() == null) {
            throw new BadRequestException("Customer id is required");
        }

        Address address = new Address();
        address.setCustomerId(request.getCustomerId());
        address.setFullName(request.getFullName());
        address.setMobileNumber(request.getMobileNumber());
        address.setFlatNumber(request.getFlatNumber());
        address.setCity(request.getCity());
        address.setPincode(request.getPincode());
        address.setState(request.getState());

        return mapToAddressResponse(addressRepository.save(address));
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        return mapToOrderResponse(getOrderEntity(orderId));
    }

    @Override
    public boolean hasUserPurchasedBook(Long userId, Long productId) {
        return orderRepository.findByUserId(userId).stream()
                .anyMatch(order -> order.getProductId().equals(productId)
                        && !"CANCELLED".equalsIgnoreCase(order.getOrderStatus()));
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("User id is required");
        }
        if (request.getAmountPaid() == null || request.getAmountPaid().doubleValue() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
        if (request.getProductId() == null) {
            throw new BadRequestException("Product id is required");
        }
        if (request.getAddressId() == null) {
            throw new BadRequestException("Address id is required");
        }
    }

    private void processRefund(Order order) {
        WalletTransactionRequest refundRequest = new WalletTransactionRequest();
        refundRequest.setAmount(order.getAmountPaid());
        refundRequest.setOrderId(order.getOrderId());
        refundRequest.setRemarks("Refund for cancelled order " + order.getOrderId());

        if (order.getWalletId() == null) {
            throw new BadRequestException("Wallet id not found for refund");
        }

        walletClient.refundMoney(order.getWalletId(), refundRequest);
    }

    private void sendNotification(Long userId, String type, String message) {
        NotificationEvent event = new NotificationEvent();
        event.setUserId(userId);
        event.setType(type);
        event.setMessage(message);

        notificationEventProducer.publish(event);
    }

    private Address getAddress(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
    }

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setUserId(order.getUserId());
        response.setOrderDate(order.getOrderDate());
        response.setAmountPaid(order.getAmountPaid());
        response.setModeOfPayment(order.getModeOfPayment());
        response.setOrderStatus(order.getOrderStatus());
        response.setQuantity(order.getQuantity());
        response.setProductId(order.getProductId());
        response.setProductName(order.getProductName());
        response.setWalletId(order.getWalletId());
        response.setAddress(mapToAddressResponse(order.getAddress()));
        return response;
    }

    private AddressResponse mapToAddressResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setAddressId(address.getAddressId());
        response.setCustomerId(address.getCustomerId());
        response.setFullName(address.getFullName());
        response.setMobileNumber(address.getMobileNumber());
        response.setFlatNumber(address.getFlatNumber());
        response.setCity(address.getCity());
        response.setPincode(address.getPincode());
        response.setState(address.getState());
        return response;
    }
}
