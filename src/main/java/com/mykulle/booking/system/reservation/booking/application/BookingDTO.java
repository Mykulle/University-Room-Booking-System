package com.mykulle.booking.system.reservation.booking.application;

import java.time.LocalDateTime;

public record BookingDTO(
        Long id,
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {
}
