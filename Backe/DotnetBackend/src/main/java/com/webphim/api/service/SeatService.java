package com.webphim.api.service;

import com.webphim.api.dto.RoomDtos.*;
import com.webphim.api.entity.Seat;
import com.webphim.api.repository.RoomRepository;
import com.webphim.api.repository.SeatRepository;
import com.webphim.api.repository.SeatTypeRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatService {
    private final SeatRepository seats;
    private final SeatTypeRepository seatTypes;
    private final RoomRepository rooms;

    public SeatService(SeatRepository seats, SeatTypeRepository seatTypes, RoomRepository rooms) {
        this.seats = seats;
        this.seatTypes = seatTypes;
        this.rooms = rooms;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getByRoomId(Integer roomId) {
        if (!rooms.existsById(roomId)) {
            return List.of();
        }
        return seats.findByRoomRoomIdOrderByRowNameAscSeatNumberAsc(roomId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatTypeResponse> getAllSeatTypes() {
        return seatTypes.findAllByOrderBySeatTypeIdAsc().stream()
                .map(st -> new SeatTypeResponse(st.getSeatTypeId(), st.getName(), st.getSurcharge()))
                .toList();
    }

    @Transactional
    public int updateSeatMap(Integer roomId, UpdateSeatMapRequest request) {
        if (request.seats() == null || request.seats().isEmpty()) {
            throw new IllegalArgumentException("Danh sách ghế cần cập nhật không được rỗng.");
        }

        var typeIds = request.seats().stream()
                .filter(s -> s.seatTypeId() != null && s.seatTypeId() != -1)
                .map(SeatUpdateItem::seatTypeId)
                .collect(java.util.stream.Collectors.toSet());
        var validTypeIds = new HashSet<>(seatTypes.findAllById(typeIds).stream().map(st -> st.getSeatTypeId()).toList());
        var invalidTypeIds = typeIds.stream().filter(id -> !validTypeIds.contains(id)).toList();
        if (!invalidTypeIds.isEmpty()) {
            throw new IllegalArgumentException("Loại ghế không hợp lệ: " + invalidTypeIds);
        }

        var deleteIds = request.seats().stream().filter(s -> s.seatTypeId() != null && s.seatTypeId() == -1).map(SeatUpdateItem::seatId).toList();
        var updateItems = request.seats().stream().filter(s -> s.seatTypeId() != null && s.seatTypeId() != -1).toList();
        var processed = 0;

        if (!deleteIds.isEmpty()) {
            var seatsToDelete = seats.findByRoomRoomIdAndSeatIdIn(roomId, deleteIds);
            processed += seatsToDelete.size();
            seats.deleteAll(seatsToDelete);
        }

        if (!updateItems.isEmpty()) {
            var updateMap = new HashMap<Integer, Integer>();
            updateItems.forEach(item -> updateMap.put(item.seatId(), item.seatTypeId()));
            var seatsToUpdate = seats.findByRoomRoomIdAndSeatIdIn(roomId, updateMap.keySet());
            var seatTypeMap = seatTypes.findAllById(updateMap.values()).stream()
                    .collect(java.util.stream.Collectors.toMap(st -> st.getSeatTypeId(), st -> st));
            for (Seat seat : seatsToUpdate) {
                seat.setSeatType(seatTypeMap.get(updateMap.get(seat.getSeatId())));
            }
            processed += seatsToUpdate.size();
        }

        return processed;
    }

    @Transactional
    public int initializeSeatMap(Integer roomId, GenerateSeatMapRequest request) {
        var room = rooms.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Phòng chiếu không tồn tại."));
        if (seats.existsByRoomRoomId(roomId)) {
            throw new IllegalStateException("Phòng chiếu này đã có sơ đồ ghế.");
        }
        var defaultType = seatTypes.findAllByOrderBySeatTypeIdAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hệ thống chưa cấu hình loại ghế nào (SeatType)."));
        var newSeats = new java.util.ArrayList<Seat>();
        for (int r = 0; r < request.rows(); r++) {
            var rowName = String.valueOf((char) ('A' + r));
            for (int c = 1; c <= request.columns(); c++) {
                var seat = new Seat();
                seat.setRoom(room);
                seat.setSeatType(defaultType);
                seat.setRowName(rowName);
                seat.setSeatNumber(c);
                newSeats.add(seat);
            }
        }
        seats.saveAll(newSeats);
        room.setTotalSeats(newSeats.size());
        return newSeats.size();
    }

    @Transactional
    public boolean deleteAllByRoomId(Integer roomId) {
        if (!rooms.existsById(roomId)) {
            throw new IllegalArgumentException("Phòng chiếu không tồn tại.");
        }
        var roomSeats = seats.findByRoomRoomId(roomId);
        if (roomSeats.isEmpty()) {
            return false;
        }
        seats.deleteAll(roomSeats);
        rooms.findById(roomId).ifPresent(room -> room.setTotalSeats(0));
        return true;
    }

    private SeatResponse toResponse(Seat seat) {
        var st = seat.getSeatType();
        return new SeatResponse(seat.getSeatId(), seat.getRoom().getRoomId(), seat.getRowName(), seat.getSeatNumber(),
                new SeatTypeResponse(st.getSeatTypeId(), st.getName(), st.getSurcharge()));
    }
}
