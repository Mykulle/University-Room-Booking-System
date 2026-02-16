package com.mykulle.booking.system.reservation.booking.application;

import java.time.LocalDateTime;

public record RoomAvailabilityDTO(
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {
}
