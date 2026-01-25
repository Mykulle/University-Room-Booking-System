package com.mykulle.booking.system.rooms;

public record RoomDTO(
        Long id,
        String name,
        String building,
        String level,
        String roomCode,
        Room.RoomStatus status) {
}
