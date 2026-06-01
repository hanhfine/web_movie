package com.webphim.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class MovieDtos {
    private MovieDtos() {}

    public record MovieResponse(
            @JsonProperty("id") Integer movieId,
            String title,
            String description,
            int duration,
            String poster,
            String banner,
            String genre,
            String releaseDate,
            String director,
            String castMembers,
            String ageRating,
            double rating,
            String status,
            boolean isDeleted) {}

    public record MoviePagedResponse(
            List<MovieResponse> items,
            long totalCount,
            int pageNumber,
            int pageSize,
            int totalPages) {}

    public record CreateMovieRequest(
            String title,
            String description,
            int duration,
            String poster,
            String banner,
            String genre,
            String releaseDate,
            String director,
            String castMembers,
            String ageRating,
            String status) {
        public String genreOrDefault() { return genre == null || genre.isBlank() ? "Chưa cập nhật" : genre; }
        public String releaseDateOrDefault() { return releaseDate == null || releaseDate.isBlank() ? "Chưa cập nhật" : releaseDate; }
        public String ageRatingOrDefault() { return ageRating == null || ageRating.isBlank() ? "P" : ageRating; }
        public String statusOrDefault() { return status == null || status.isBlank() ? "showing" : status; }
    }
}
