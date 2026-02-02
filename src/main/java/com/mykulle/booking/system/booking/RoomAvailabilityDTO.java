package com.mykulle.booking.system.booking;

import java.time.LocalTime;
import java.util.List;

public record RoomAvailabilityDTO(
        String roomLocation,
        String roomName,
        List<SlotDTO> slots
) {
    public record SlotDTO(LocalTime start, LocalTime end, boolean available) {}
}
