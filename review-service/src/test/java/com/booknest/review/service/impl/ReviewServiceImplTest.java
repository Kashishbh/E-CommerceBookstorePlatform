package com.booknest.review.service.impl;

import com.booknest.review.client.OrderClient;
import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.entity.Review;
import com.booknest.review.exception.BadRequestException;
import com.booknest.review.exception.ResourceNotFoundException;
import com.booknest.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void addReview_shouldSaveReviewWhenUserPurchasedBook() {
        ReviewRequest request = buildReviewRequest();

        when(orderClient.hasUserPurchasedBook(1L, 1L)).thenReturn(true);
        when(reviewRepository.findByBookIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(1L);
            return review;
        });

        ReviewResponse response = reviewService.addReview(request);

        assertNotNull(response);
        assertEquals(1L, response.getReviewId());
        assertEquals(1L, response.getBookId());
        assertEquals(1L, response.getUserId());
        assertEquals(5, response.getRating());
        assertEquals("Excellent book", response.getComment());

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void addReview_shouldThrowExceptionWhenUserNotPurchasedBook() {
        ReviewRequest request = buildReviewRequest();

        when(orderClient.hasUserPurchasedBook(1L, 1L)).thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.addReview(request)
        );

        assertEquals("Only purchased users can review this book", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_shouldThrowExceptionWhenDuplicateReviewExists() {
        ReviewRequest request = buildReviewRequest();
        Review existing = buildReview();

        when(orderClient.hasUserPurchasedBook(1L, 1L)).thenReturn(true);
        when(reviewRepository.findByBookIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.addReview(request)
        );

        assertEquals("User has already reviewed this book", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_shouldThrowExceptionWhenRatingInvalid() {
        ReviewRequest request = buildReviewRequest();
        request.setRating(0);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.addReview(request)
        );

        assertEquals("Rating must be between 1 and 5", exception.getMessage());
        verify(orderClient, never()).hasUserPurchasedBook(anyLong(), anyLong());
    }

    @Test
    void getByBook_shouldReturnReviews() {
        Review review = buildReview();
        review.setReviewId(1L);

        when(reviewRepository.findByBookId(1L)).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getByBook(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getBookId());
    }

    @Test
    void getByUser_shouldReturnReviews() {
        Review review = buildReview();
        review.setReviewId(1L);

        when(reviewRepository.findByUserId(1L)).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getByUser(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getUserId());
    }

    @Test
    void updateReview_shouldUpdateSuccessfully() {
        Review existing = buildReview();
        existing.setReviewId(1L);

        ReviewRequest request = buildReviewRequest();
        request.setComment("Updated review");
        request.setRating(4);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.updateReview(1L, request);

        assertEquals("Updated review", response.getComment());
        assertEquals(4, response.getRating());
    }

    @Test
    void updateReview_shouldThrowExceptionWhenNotFound() {
        ReviewRequest request = buildReviewRequest();

        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.updateReview(99L, request));
    }

    @Test
    void deleteReview_shouldDeleteSuccessfully() {
        Review review = buildReview();
        review.setReviewId(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_shouldThrowExceptionWhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.deleteReview(99L));
    }

    @Test
    void getAvgRating_shouldReturnValueFromRepository() {
        when(reviewRepository.avgRatingByBookId(1L)).thenReturn(4.5);

        Double avg = reviewService.getAvgRating(1L);

        assertEquals(4.5, avg);
    }

    @Test
    void getAvgRating_shouldReturnZeroWhenNullReturned() {
        when(reviewRepository.avgRatingByBookId(1L)).thenReturn(null);

        Double avg = reviewService.getAvgRating(1L);

        assertEquals(0.0, avg);
    }

    @Test
    void getAllReviews_shouldReturnAllReviews() {
        Review review = buildReview();
        review.setReviewId(1L);

        when(reviewRepository.findAll()).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getAllReviews();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getReviewId());
    }

    @Test
    void getReviewById_shouldReturnReviewSuccessfully() {
        Review review = buildReview();
        review.setReviewId(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getReviewById(1L);

        assertEquals(1L, response.getReviewId());
        assertEquals("Excellent book", response.getComment());
    }

    @Test
    void getReviewById_shouldThrowExceptionWhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewById(99L));
    }

    @Test
    void addReview_shouldCaptureSavedReviewValues() {
        ReviewRequest request = buildReviewRequest();

        when(orderClient.hasUserPurchasedBook(1L, 1L)).thenReturn(true);
        when(reviewRepository.findByBookIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.addReview(request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());

        Review savedReview = captor.getValue();
        assertEquals(1L, savedReview.getBookId());
        assertEquals(1L, savedReview.getUserId());
        assertEquals(5, savedReview.getRating());
        assertEquals("Excellent book", savedReview.getComment());
    }

    private ReviewRequest buildReviewRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setBookId(1L);
        request.setUserId(1L);
        request.setRating(5);
        request.setComment("Excellent book");
        request.setVerified(true);
        return request;
    }

    private Review buildReview() {
        Review review = new Review();
        review.setReviewId(1L);
        review.setBookId(1L);
        review.setUserId(1L);
        review.setRating(5);
        review.setComment("Excellent book");
        review.setVerified(true);
        return review;
    }
}
