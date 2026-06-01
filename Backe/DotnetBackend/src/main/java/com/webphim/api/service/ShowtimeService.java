package com.webphim.api.service;

import com.webphim.api.dto.ShowtimeDtos.*;
import com.webphim.api.entity.Movie;
import com.webphim.api.entity.Room;
import com.webphim.api.entity.Showtime;
import com.webphim.api.entity.ShowtimeStatus;
import com.webphim.api.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShowtimeService {
    private final ShowtimeRepository showtimes;
    private final MovieRepository movies;
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final BookingSeatRepository bookingSeats;
    private final StringRedisTemplate redis;

    public ShowtimeService(ShowtimeRepository showtimes, MovieRepository movies, RoomRepository rooms,
                           SeatRepository seats, BookingSeatRepository bookingSeats, StringRedisTemplate redis) {
        this.showtimes = showtimes;
        this.movies = movies;
        this.rooms = rooms;
        this.seats = seats;
        this.bookingSeats = bookingSeats;
        this.redis = redis;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeDetailResponse> getAll() {
        return showtimes.findClientShowtimes(ShowtimeStatus.active).stream().map(this::toDetailResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeDetailResponse> getByMovie(Integer movieId) {
        return showtimes.findClientShowtimesByMovie(movieId, ShowtimeStatus.active).stream().map(this::toDetailResponse).toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ShowtimeDetailResponse> getDetail(Integer id) {
        return showtimes.findDetailById(id).map(this::toDetailResponse);
    }

    @Transactional(readOnly = true)
    public List<SeatStatusResponse> getSeatStatuses(Integer showtimeId) {
        var showtime = showtimes.findDetailById(showtimeId)
                .orElseThrow(() -> new NoSuchElementException("Suất chiếu " + showtimeId + " không tồn tại"));
        var redisKey = "showtime:" + showtimeId + ":seats";
        return seats.findByRoomRoomIdOrderByRowNameAscSeatNumberAsc(showtime.getRoom().getRoomId()).stream()
                .map(seat -> {
                    var seatKey = seat.getRowName() + seat.getSeatNumber();
                    var status = redis.opsForHash().get(redisKey, seatKey);
                    var actualStatus = status == null ? "AVAILABLE" : status.toString();
                    var price = showtime.getBasePrice().add(seat.getSeatType().getSurcharge());
                    return new SeatStatusResponse(seat.getSeatId(), seat.getRowName(), seat.getSeatNumber(),
                            actualStatus, seat.getSeatType().getName(), price);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminShowtimeResponse> getAdminShowtimes(LocalDate date, Integer roomId) {
        LocalDateTime start = date == null ? null : date.atStartOfDay();
        LocalDateTime end = date == null ? null : date.atTime(LocalTime.MAX);
        return showtimes.findAdminShowtimes(start, end, roomId).stream()
                .map(s -> toAdminResponse(s, (int) bookingSeats.countByBookingShowtimeShowtimeId(s.getShowtimeId()),
                        (int) seats.countByRoomRoomId(s.getRoom().getRoomId())))
                .toList();
    }

    @Transactional
    public AdminShowtimeResponse create(CreateShowtimeRequest request) {
        var movie = movies.findById(request.movieId()).orElseThrow(() -> new IllegalArgumentException("Phim không tồn tại."));
        var room = rooms.findById(request.roomId()).orElseThrow(() -> new IllegalArgumentException("Phòng chiếu không tồn tại."));
        var endTime = computeEndTime(movie, request.startTime(), request.bufferTimeMinutesOrDefault());
        checkOverlap(room.getRoomId(), request.startTime(), endTime, null);
        var showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(endTime);
        showtime.setBasePrice(request.basePrice());
        showtime.setBufferTimeMinutes(request.bufferTimeMinutesOrDefault());
        showtime.setStatus(ShowtimeStatus.draft);
        showtimes.save(showtime);
        return toAdminResponse(showtime, 0, (int) seats.countByRoomRoomId(room.getRoomId()));
    }

    @Transactional
    public java.util.Optional<AdminShowtimeResponse> update(Integer id, UpdateShowtimeRequest request) {
        var showtime = showtimes.findDetailById(id);
        if (showtime.isEmpty()) {
            return java.util.Optional.empty();
        }
        var movie = movies.findById(request.movieId()).orElseThrow(() -> new IllegalArgumentException("Phim không tồn tại."));
        var room = rooms.findById(request.roomId()).orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
        var endTime = computeEndTime(movie, request.startTime(), request.bufferTimeMinutesOrDefault());
        checkOverlap(room.getRoomId(), request.startTime(), endTime, id);
        var s = showtime.get();
        s.setMovie(movie);
        s.setRoom(room);
        s.setStartTime(request.startTime());
        s.setEndTime(endTime);
        s.setBasePrice(request.basePrice());
        s.setBufferTimeMinutes(request.bufferTimeMinutesOrDefault());
        invalidateShowtimeRedis(id);
        return java.util.Optional.of(toAdminResponse(s, 0, (int) seats.countByRoomRoomId(room.getRoomId())));
    }

    @Transactional
    public boolean cancel(Integer id) {
        var showtime = showtimes.findById(id);
        if (showtime.isEmpty()) {
            return false;
        }
        showtime.get().setStatus(ShowtimeStatus.cancelled);
        invalidateShowtimeRedis(id);
        return true;
    }

    @Transactional
    public boolean publish(Integer id) {
        var showtime = showtimes.findById(id);
        if (showtime.isEmpty()) {
            return false;
        }
        if (showtime.get().getStatus() != ShowtimeStatus.draft) {
            throw new IllegalStateException("Chỉ có thể xuất bản suất chiếu đang ở trạng thái nháp.");
        }
        showtime.get().setStatus(ShowtimeStatus.active);
        return true;
    }

    @Transactional
    public AutoGenerateResult autoGenerate(AutoGenerateRequest request) {
        var movie = movies.findById(request.movieId()).orElseThrow(() -> new IllegalArgumentException("Phim không tồn tại."));
        var movieDuration = movie.getDuration() > 0 ? movie.getDuration() : 90;
        var created = 0;
        var conflicts = new ArrayList<String>();
        var cleaningMinutes = request.cleaningMinutesOrDefault();

        for (Integer roomId : request.roomIds()) {
            var room = rooms.findById(roomId).orElse(null);
            if (room == null) {
                continue;
            }
            var current = request.date().atTime(request.openTime());
            var closeTime = request.date().atTime(request.closeTime());
            while (!current.plusMinutes(movieDuration).isAfter(closeTime)) {
                var endWithBuffer = current.plusMinutes(movieDuration + cleaningMinutes);
                var conflict = showtimes.findOverlap(roomId, current, endWithBuffer, null);
                if (conflict.isPresent()) {
                    var conflictEnd = conflict.get().getEndTime() == null ? conflict.get().getStartTime().plusMinutes(movieDuration) : conflict.get().getEndTime();
                    conflicts.add("Phòng " + room.getName() + ": bỏ qua " + current.toLocalTime() + " do vướng suất " + conflict.get().getStartTime().toLocalTime() + ".");
                    current = conflictEnd.plusMinutes(cleaningMinutes);
                    continue;
                }
                var showtime = new Showtime();
                showtime.setMovie(movie);
                showtime.setRoom(room);
                showtime.setStartTime(current);
                showtime.setEndTime(current.plusMinutes(movieDuration));
                showtime.setBasePrice(request.basePrice());
                showtime.setBufferTimeMinutes(cleaningMinutes);
                showtime.setStatus(ShowtimeStatus.draft);
                showtimes.save(showtime);
                created++;
                current = endWithBuffer;
            }
        }
        return new AutoGenerateResult(created, conflicts);
    }

    private LocalDateTime computeEndTime(Movie movie, LocalDateTime start, int bufferMinutes) {
        var duration = (movie.getDuration() > 0 ? movie.getDuration() : 90) + bufferMinutes;
        return start.plusMinutes(duration);
    }

    private void checkOverlap(Integer roomId, LocalDateTime start, LocalDateTime end, Integer excludeId) {
        var conflict = showtimes.findOverlap(roomId, start, end, excludeId);
        if (conflict.isPresent()) {
            throw new IllegalStateException("Phòng trùng lịch với suất: "
                    + conflict.get().getStartTime().toLocalTime() + "-" + conflict.get().getEndTime().toLocalTime() + ".");
        }
    }

    private void invalidateShowtimeRedis(Integer showtimeId) {
        redis.delete("showtime:" + showtimeId + ":seats");
    }

    private ShowtimeDetailResponse toDetailResponse(Showtime s) {
        return new ShowtimeDetailResponse(s.getShowtimeId(), s.getMovie().getMovieId(), s.getMovie().getTitle(),
                s.getMovie().getPoster(), s.getRoom().getName(), s.getStartTime(), s.getBasePrice(),
                s.getMovie().getDuration(), s.getMovie().getGenre(), s.getMovie().getAgeRating());
    }

    private AdminShowtimeResponse toAdminResponse(Showtime s, int soldSeats, int totalSeats) {
        return new AdminShowtimeResponse(s.getShowtimeId(), s.getMovie().getMovieId(), s.getMovie().getTitle(),
                s.getMovie().getPoster(), s.getRoom().getRoomId(), s.getRoom().getName(), s.getStartTime(),
                s.getEndTime(), s.getBasePrice(), s.getStatus().name(), soldSeats, totalSeats, s.getMovie().getDuration());
    }
}
