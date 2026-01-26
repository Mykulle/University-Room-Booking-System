package com.mykulle.booking.system.booking;


import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingManagement {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    private static final long REQUIRED_DURATION_HOURS = 1;
    private static final long BUFFER_MINUTES = 30;

    /* Create a new booking */
    public BookingDTO createBooking(String roomLocation, LocalDateTime startTime, LocalDateTime endTime) {
        validateBookingRequest(roomLocation, startTime, endTime);

        var booking = new Booking(roomLocation, new Booking.TimeSlot(startTime, endTime));
        return bookingMapper.toDTO(bookingRepository.save(booking));
    }

    /* Get Booking by ID */
    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        return bookingMapper.toDTO(booking);
    }

    /* Get all bookings */
    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(bookingMapper::toDTO)
                .toList();
    }

    /* Check-in to a booking */
    public BookingDTO checkInBooking(Long id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        return bookingMapper.toDTO(bookingRepository.save(booking.checkIn()));
    }

    /* Cancel a Booking */
    public BookingDTO cancelBooking(Long id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        return bookingMapper.toDTO(bookingRepository.save(booking.cancel()));
    }

    private void validateBookingRequest(String roomLocation, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeSlotDuration(startTime, endTime);
        validateNoBookingConflicts(roomLocation, startTime, endTime);
    }

    private void validateTimeSlotDuration(LocalDateTime startTime, LocalDateTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();

        if (hours != REQUIRED_DURATION_HOURS) {
            throw new RuntimeException("Booking must be exactly " + REQUIRED_DURATION_HOURS + " hour(s) long");
        }
    }

    private void validateNoBookingConflicts(String roomLocation, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime bufferStart = startTime.minusMinutes(BUFFER_MINUTES);
        LocalDateTime bufferEnd = endTime.plusMinutes(BUFFER_MINUTES);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                roomLocation,
                bufferStart,
                bufferEnd
        );

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Booking conflicts with existing bookings for the selected room and time slot");
        }
    }
}
