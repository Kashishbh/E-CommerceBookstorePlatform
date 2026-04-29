package com.booknest.order.client;

import com.booknest.order.client.dto.BookResponse;
import com.booknest.order.client.dto.StockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "CATALOG-SERVICE")
public interface CatalogClient {

    @GetMapping("/books/{id}")
    BookResponse getBookById(@PathVariable("id") Long id);

    @PutMapping("/books/{id}/stock")
    BookResponse updateStock(@PathVariable("id") Long id,
                             @RequestBody StockUpdateRequest request);
}
