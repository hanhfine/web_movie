package com.webphim.api.service;

import com.webphim.api.dto.MovieDtos.*;
import com.webphim.api.entity.Movie;
import com.webphim.api.entity.MovieStatus;
import com.webphim.api.entity.OrderStatus;
import com.webphim.api.entity.ShowtimeStatus;
import com.webphim.api.repository.MovieRepository;
import com.webphim.api.repository.ShowtimeRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;

    public MovieService(MovieRepository movies, ShowtimeRepository showtimes) {
        this.movies = movies;
        this.showtimes = showtimes;
    }

    @Transactional(readOnly = true)
    public MoviePagedResponse getPaged(int pageNumber, int pageSize, String search, String status) {
        var pageable = PageRequest.of(Math.max(pageNumber, 1) - 1, Math.max(pageSize, 1), Sort.by(Sort.Direction.DESC, "movieId"));
        MovieStatus parsedStatus = parseStatusOrNull(status);
        var page = (search != null && !search.isBlank() && parsedStatus != null)
                ? movies.findByDeletedFalseAndTitleContainingIgnoreCaseAndStatus(search, parsedStatus, pageable)
                : (search != null && !search.isBlank())
                    ? movies.findByDeletedFalseAndTitleContainingIgnoreCase(search, pageable)
                    : parsedStatus != null
                        ? movies.findByDeletedFalseAndStatus(parsedStatus, pageable)
                        : movies.findByDeletedFalse(pageable);
        return new MoviePagedResponse(page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(), pageNumber, pageSize, page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getAll() {
        return movies.findByDeletedFalseAndStatusNot(MovieStatus.draft).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getByStatus(String status) {
        MovieStatus parsedStatus = parseStatusOrNull(status);
        if (parsedStatus == null) {
            return List.of();
        }
        return movies.findByDeletedFalseAndStatus(parsedStatus).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MovieResponse> getById(Integer id) {
        return movies.findByMovieIdAndDeletedFalse(id).map(this::toResponse);
    }

    @Transactional
    public MovieResponse create(CreateMovieRequest request) {
        validateDuration(request.duration(), "Thời lượng phim (Duration) phải nằm trong khoảng 1-500 phút.");
        if (movies.existsByTitleAndDeletedFalse(request.title())) {
            throw new IllegalArgumentException("Phim có tên '" + request.title() + "' đã tồn tại trong hệ thống.");
        }
        var movie = new Movie();
        applyMovie(movie, request);
        movie.setStatus(MovieStatus.draft);
        return toResponse(movies.save(movie));
    }

    @Transactional
    public Optional<MovieResponse> update(Integer id, CreateMovieRequest request) {
        var movie = movies.findByMovieIdAndDeletedFalse(id);
        if (movie.isEmpty()) {
            return Optional.empty();
        }
        validateDuration(request.duration(), "Thời lượng phải nằm trong khoảng 1-500 phút.");
        if (movies.existsByTitleAndMovieIdNotAndDeletedFalse(request.title(), id)) {
            throw new IllegalArgumentException("Phim có tên '" + request.title() + "' đã tồn tại.");
        }
        var status = parseStatusOrThrow(request.statusOrDefault(), "Trạng thái không hợp lệ.");
        applyMovie(movie.get(), request);
        movie.get().setStatus(status);
        return Optional.of(toResponse(movie.get()));
    }

    @Transactional
    public boolean softDelete(Integer id) {
        var movie = movies.findByMovieIdAndDeletedFalse(id);
        if (movie.isEmpty()) {
            return false;
        }
        var hasActiveBookings = showtimes.findClientShowtimes(ShowtimeStatus.active).stream()
                .filter(s -> s.getMovie().getMovieId().equals(id) && s.getStartTime().isAfter(LocalDateTime.now()))
                .flatMap(s -> s.getBookings().stream())
                .anyMatch(b -> b.getOrder().getStatus() == OrderStatus.pending || b.getOrder().getStatus() == OrderStatus.paid);
        if (hasActiveBookings) {
            throw new IllegalStateException("KHÔNG THỂ XÓA: Phim vẫn còn suất chiếu trong tương lai đã phát sinh đơn hàng.");
        }
        movie.get().setDeleted(true);
        return true;
    }

    @Transactional
    public boolean publish(Integer id, String targetStatus) {
        var movie = movies.findByMovieIdAndDeletedFalse(id);
        if (movie.isEmpty()) {
            return false;
        }
        if (movie.get().getStatus() != MovieStatus.draft) {
            throw new IllegalStateException("Chỉ có thể xuất bản phim đang ở trạng thái nháp.");
        }
        var status = parseStatusOrThrow(targetStatus, "Trạng thái sau xuất bản phải là 'showing' hoặc 'coming'.");
        if (status == MovieStatus.draft) {
            throw new IllegalStateException("Trạng thái sau xuất bản phải là 'showing' hoặc 'coming'.");
        }
        movie.get().setStatus(status);
        return true;
    }

    private void applyMovie(Movie movie, CreateMovieRequest request) {
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setDuration(request.duration());
        movie.setPoster(request.poster());
        movie.setBanner(request.banner());
        movie.setGenre(request.genreOrDefault());
        movie.setReleaseDate(request.releaseDateOrDefault());
        movie.setDirector(request.director());
        movie.setCastMembers(request.castMembers());
        movie.setAgeRating(request.ageRatingOrDefault());
    }

    private void validateDuration(int duration, String message) {
        if (duration != 0 && (duration < 1 || duration > 500)) {
            throw new IllegalArgumentException(message);
        }
    }

    private MovieStatus parseStatusOrThrow(String status, String message) {
        try {
            return MovieStatus.valueOf(status);
        } catch (Exception ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private MovieStatus parseStatusOrNull(String status) {
        try {
            return status == null || status.isBlank() ? null : MovieStatus.valueOf(status);
        } catch (Exception ex) {
            return null;
        }
    }

    private MovieResponse toResponse(Movie m) {
        return new MovieResponse(m.getMovieId(), m.getTitle(), m.getDescription(), m.getDuration(), m.getPoster(),
                m.getBanner(), m.getGenre(), m.getReleaseDate(), m.getDirector(), m.getCastMembers(),
                m.getAgeRating(), m.getRating(), m.getStatus().name(), m.isDeleted());
    }
}
