package com.mykulle.booking.system.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingDTO (
        Long id,
        String roomLocation,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDate bookingDate,
        Booking.BookingStatus status
) {}