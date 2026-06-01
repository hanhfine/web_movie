package com.webphim.api.service;

import com.webphim.api.dto.RoomDtos.*;
import com.webphim.api.entity.Room;
import com.webphim.api.entity.RoomStatus;
import com.webphim.api.repository.RoomRepository;
import com.webphim.api.repository.SeatRepository;
import com.webphim.api.repository.ShowtimeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final ShowtimeRepository showtimes;

    public RoomService(RoomRepository rooms, SeatRepository seats, ShowtimeRepository showtimes) {
        this.rooms = rooms;
        this.seats = seats;
        this.showtimes = showtimes;
    }

    @Transactional(readOnly = true)
    public RoomPagedResponse getPaged(int pageNumber, int pageSize, String search) {
        var pageable = PageRequest.of(Math.max(pageNumber, 1) - 1, Math.max(pageSize, 1), Sort.by("roomId"));
        var page = search == null || search.isBlank() ? rooms.findAll(pageable) : rooms.findByNameContainingIgnoreCase(search, pageable);
        return new RoomPagedResponse(page.getContent().stream().map(this::toResponseWithSeatCount).toList(),
                page.getTotalElements(), pageNumber, pageSize, page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAll() {
        return rooms.findAllByOrderByNameAsc().stream().map(this::toResponseWithSeatCount).toList();
    }

    @Transactional(readOnly = true)
    public Optional<RoomResponse> getById(Integer id) {
        return rooms.findById(id).map(this::toResponseWithSeatCount);
    }

    @Transactional
    public RoomResponse create(CreateRoomRequest request) {
        if (rooms.existsByName(request.name())) {
            throw new IllegalArgumentException("Phòng chiếu có tên '" + request.name() + "' đã tồn tại.");
        }
        var room = new Room();
        room.setName(request.name());
        room.setStatus(parseStatus(request.statusOrDefault()));
        room.setTotalSeats(0);
        return toResponseWithSeatCount(rooms.save(room));
    }

    @Transactional
    public Optional<RoomResponse> update(Integer id, CreateRoomRequest request) {
        var room = rooms.findById(id);
        if (room.isEmpty()) {
            return Optional.empty();
        }
        if (rooms.existsByNameAndRoomIdNot(request.name(), id)) {
            throw new IllegalArgumentException("Phòng chiếu có tên '" + request.name() + "' đã tồn tại.");
        }
        room.get().setName(request.name());
        room.get().setStatus(parseStatus(request.statusOrDefault()));
        return Optional.of(toResponseWithSeatCount(room.get()));
    }

    @Transactional
    public boolean delete(Integer id) {
        var room = rooms.findById(id);
        if (room.isEmpty()) {
            return false;
        }
        if (showtimes.existsByRoomRoomId(id)) {
            throw new IllegalStateException("Không thể xóa phòng vì phòng này đang có suất chiếu liên kết.");
        }
        seats.deleteAll(seats.findByRoomRoomId(id));
        rooms.delete(room.get());
        return true;
    }

    private RoomStatus parseStatus(String status) {
        try {
            return RoomStatus.valueOf(status);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active' hoặc 'maintenance'.");
        }
    }

    private RoomResponse toResponseWithSeatCount(Room room) {
        return new RoomResponse(room.getRoomId(), room.getName(), (int) seats.countByRoomRoomId(room.getRoomId()), room.getStatus().name());
    }
}
