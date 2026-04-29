package com.booknest.review.service;

import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse addReview(ReviewRequest request);
    List<ReviewResponse> getByBook(Long bookId);
    List<ReviewResponse> getByUser(Long userId);
    ReviewResponse updateReview(Long reviewId, ReviewRequest request);
    void deleteReview(Long reviewId);
    Double getAvgRating(Long bookId);
    List<ReviewResponse> getAllReviews();
    ReviewResponse getReviewById(Long reviewId);
}
