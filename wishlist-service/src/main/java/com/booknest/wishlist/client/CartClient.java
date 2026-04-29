package com.booknest.wishlist.client;

import com.booknest.wishlist.client.dto.CartItemRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {

    @PostMapping("/cart/{userId}/items")
    Object addItem(@PathVariable("userId") Long userId,
                   @RequestBody CartItemRequest request);
}
