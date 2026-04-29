package com.booknest.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {

    @GetMapping("/orders/user/{userId}/product/{productId}/purchased")
    boolean hasUserPurchasedBook(@PathVariable("userId") Long userId,
                                 @PathVariable("productId") Long productId);
}
