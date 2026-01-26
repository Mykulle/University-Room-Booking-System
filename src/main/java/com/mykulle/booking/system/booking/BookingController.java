package com.mykulle.booking.system.booking;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/booking")
public class BookingController {

    private final BookingManagement bookings;

    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@RequestBody CreateBookingRequest request) {
        var booking = bookings.createBooking(
                request.roomLocation(),
                request.startTime(),
                request.endTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBooking(@PathVariable Long id) {
        var booking = bookings.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        var allBookings = bookings.getAllBookings();
        return ResponseEntity.ok(allBookings);
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<BookingDTO> checkIn(@PathVariable Long id) {
        var booking = bookings.checkInBooking(id);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDTO> cancel(@PathVariable Long id) {
        var booking = bookings.cancelBooking(id);
        return ResponseEntity.ok(booking);
    }

    public record CreateBookingRequest(
            String roomLocation,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {}
}
