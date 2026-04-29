package com.booknest.review.repository;

import com.booknest.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookId(Long bookId);
    List<Review> findByUserId(Long userId);
    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);
    void deleteByReviewId(Long reviewId);

    @Query("select avg(r.rating) from Review r where r.bookId = :bookId")
    Double avgRatingByBookId(Long bookId);

    long countByBookId(Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}
