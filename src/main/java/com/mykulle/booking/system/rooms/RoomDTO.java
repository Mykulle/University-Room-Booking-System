package com.mykulle.booking.system.rooms;

public record RoomDTO(
        Long id,
        String name,
        String roomLocation,
        Room.RoomStatus status) {
}
