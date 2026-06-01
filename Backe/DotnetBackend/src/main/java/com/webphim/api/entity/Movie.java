package com.webphim.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Integer movieId;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Description", columnDefinition = "text")
    private String description;

    @Column(name = "Duration")
    private Integer duration = 0;

    @Column(name = "trailer_duration_minutes")
    private Integer trailerDuration = 10;

    @Column(name = "poster_url")
    private String poster;

    @Column(name = "Banner")
    private String banner;

    @Column(name = "Genre", nullable = false)
    private String genre = "Chưa cập nhật";

    @Column(name = "release_date", nullable = false)
    private String releaseDate = "Chưa cập nhật";

    @Column(name = "Director")
    private String director;

    @Column(name = "cast_members")
    private String castMembers;

    @Column(name = "age_rating", length = 10)
    private String ageRating = "P";

    @Column(name = "Rating")
    private Double rating = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private MovieStatus status = MovieStatus.draft;

    @Column(name = "is_deleted")
    private boolean deleted = false;

    @OneToMany(mappedBy = "movie")
    private List<Showtime> showtimes = new ArrayList<>();

    public Integer getMovieId() { return movieId; }
    public void setMovieId(Integer movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDuration() { return duration == null ? 0 : duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getTrailerDuration() { return trailerDuration; }
    public void setTrailerDuration(Integer trailerDuration) { this.trailerDuration = trailerDuration; }
    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public String getCastMembers() { return castMembers; }
    public void setCastMembers(String castMembers) { this.castMembers = castMembers; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public Double getRating() { return rating == null ? 0.0 : rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public MovieStatus getStatus() { return status; }
    public void setStatus(MovieStatus status) { this.status = status; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public List<Showtime> getShowtimes() { return showtimes; }
    public void setShowtimes(List<Showtime> showtimes) { this.showtimes = showtimes; }
}
