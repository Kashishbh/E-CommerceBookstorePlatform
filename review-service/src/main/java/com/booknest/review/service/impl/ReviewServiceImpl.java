package com.booknest.review.service.impl;

import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.entity.Review;
import com.booknest.review.exception.BadRequestException;
import com.booknest.review.exception.ResourceNotFoundException;
import com.booknest.review.repository.ReviewRepository;
import com.booknest.review.service.ReviewService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.booknest.review.client.OrderClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;

    public ReviewServiceImpl(ReviewRepository reviewRepository, OrderClient orderClient) {
        this.reviewRepository = reviewRepository;
        this.orderClient = orderClient;
    }


    @Override
    public ReviewResponse addReview(ReviewRequest request) {
        validateRequest(request);
        boolean purchased = orderClient.hasUserPurchasedBook(request.getUserId(), request.getBookId());
        if (!purchased) {
            throw new BadRequestException("Only purchased users can review this book");
        }


        if (reviewRepository.findByBookIdAndUserId(request.getBookId(), request.getUserId()).isPresent()) {
            throw new BadRequestException("User has already reviewed this book");
        }

        Review review = new Review();
        review.setBookId(request.getBookId());
        review.setUserId(request.getUserId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVerified(request.getVerified() != null ? request.getVerified() : false);

        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    public List<ReviewResponse> getByBook(Long bookId) {
        return reviewRepository.findByBookId(bookId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getByUser(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        validateRequest(request);

        Review review = getReviewEntity(reviewId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVerified(request.getVerified() != null ? request.getVerified() : review.getVerified());

        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    public void deleteReview(Long reviewId) {
        Review review = getReviewEntity(reviewId);
        reviewRepository.delete(review);
    }

    @Override
    public Double getAvgRating(Long bookId) {
        Double avg = reviewRepository.avgRatingByBookId(bookId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse getReviewById(Long reviewId) {
        return mapToResponse(getReviewEntity(reviewId));
    }

    private Review getReviewEntity(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }

    private void validateRequest(ReviewRequest request) {
        if (request.getBookId() == null) throw new BadRequestException("Book id is required");
        if (request.getUserId() == null) throw new BadRequestException("User id is required");
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }

    private ReviewResponse mapToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setReviewId(review.getReviewId());
        response.setBookId(review.getBookId());
        response.setUserId(review.getUserId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setReviewDate(review.getReviewDate());
        response.setVerified(review.getVerified());
        return response;
    }
}
