package com.booknest.review.controller;

import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@CrossOrigin("*")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ReviewResponse addReview(@RequestBody ReviewRequest request) {
        return reviewService.addReview(request);
    }

    @GetMapping
    public List<ReviewResponse> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse getReviewById(@PathVariable Long reviewId) {
        return reviewService.getReviewById(reviewId);
    }

    @GetMapping("/book/{bookId}")
    public List<ReviewResponse> getByBook(@PathVariable Long bookId) {
        return reviewService.getByBook(bookId);
    }

    @GetMapping("/user/{userId}")
    public List<ReviewResponse> getByUser(@PathVariable Long userId) {
        return reviewService.getByUser(userId);
    }

    @GetMapping("/book/{bookId}/avg-rating")
    public Double getAvgRating(@PathVariable Long bookId) {
        return reviewService.getAvgRating(bookId);
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(@PathVariable Long reviewId,
                                       @RequestBody ReviewRequest request) {
        return reviewService.updateReview(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return "Review deleted successfully";
    }
}
