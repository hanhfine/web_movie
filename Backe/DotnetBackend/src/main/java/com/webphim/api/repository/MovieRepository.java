package com.webphim.api.repository;

import com.webphim.api.entity.Movie;
import com.webphim.api.entity.MovieStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
    Page<Movie> findByDeletedFalse(Pageable pageable);
    Page<Movie> findByDeletedFalseAndTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Movie> findByDeletedFalseAndStatus(MovieStatus status, Pageable pageable);
    Page<Movie> findByDeletedFalseAndTitleContainingIgnoreCaseAndStatus(String title, MovieStatus status, Pageable pageable);
    List<Movie> findByDeletedFalseAndStatusNot(MovieStatus status);
    List<Movie> findByDeletedFalseAndStatus(MovieStatus status);
    Optional<Movie> findByMovieIdAndDeletedFalse(Integer id);
    boolean existsByTitleAndDeletedFalse(String title);
    boolean existsByTitleAndMovieIdNotAndDeletedFalse(String title, Integer movieId);
}
