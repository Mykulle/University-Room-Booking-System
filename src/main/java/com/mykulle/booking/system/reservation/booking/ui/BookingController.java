package com.mykulle.booking.system.reservation.booking.ui;

import com.mykulle.booking.system.reservation.booking.application.BookingDTO;
import com.mykulle.booking.system.reservation.booking.application.BookingManagement;
import com.mykulle.booking.system.reservation.booking.application.RoomAvailabilityDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {

    private final BookingManagement bookingManagement;

    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        var booking = bookingManagement.createBooking(request.roomId(), request.startTime(), request.endTime());
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDTO> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingManagement.cancelBooking(bookingId));
    }

    @PutMapping("/{bookingId}/check-in")
    public ResponseEntity<BookingDTO> checkIn(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingManagement.checkIn(bookingId));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDTO> locateBookingById(@PathVariable Long bookingId) {
        return bookingManagement.locateBookingById(bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<BookingDTO>> fetchBookings() {
        return ResponseEntity.ok(bookingManagement.fetchBookings());
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<BookingDTO>> fetchBookingsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(bookingManagement.fetchBookingsByRoom(roomId));
    }

    @GetMapping("/availability")
    public ResponseEntity<RoomAvailabilityDTO> fetchRoomAvailability(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return ResponseEntity.ok(bookingManagement.fetchRoomAvailability(roomId, startTime, endTime));
    }

    public record CreateBookingRequest(
            @NotNull Long roomId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {}
}
