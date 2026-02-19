package com.mykulle.booking.system.reservation.booking.application;

import com.mykulle.booking.system.reservation.booking.domain.Booking;
import com.mykulle.booking.system.reservation.booking.domain.Booking.BookingStatus;
import com.mykulle.booking.system.reservation.booking.domain.BookingRepository;
import com.mykulle.booking.system.reservation.rooms.domain.RoomRepository;
import com.mykulle.booking.system.useraccount.api.AuthorizationService;
import com.mykulle.booking.system.useraccount.api.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
@RequiredArgsConstructor
public class BookingManagement {

    // Keep as constant for now. If you later want config-driven, switch to a property-backed value.
    private static final long CHECK_IN_GRACE_PERIOD_MINUTES = 15L;

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    /**
     * Creates a booking for a given room and time range.
     * Validates that the room exists, is enabled, and has no overlapping blocking bookings.
     */
    public BookingDTO createBooking(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        if (roomId == null) throw new IllegalArgumentException("roomId is required");
        var currentUser = currentUserProvider.currentUser();

        var timeRange = new Booking.TimeRange(startTime, endTime);

        if (timeRange.startTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("startTime must not be in the past");
        }

        var room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));

        if (room.isDisabled()) {
            throw new IllegalStateException("Cannot create booking for a disabled room");
        }

        if (hasOverlappingBlockingBooking(roomId, timeRange)) {
            throw new IllegalStateException("Room is not available for the requested time range");
        }

        var ownerUserId = normalizeOwnerUserId(currentUser.subject());
        var booking = new Booking(roomId, ownerUserId, timeRange);
        return mapper.toDTO(bookingRepository.save(booking));
    }

    /**
     * Cancels an existing booking by its ID.
     */
    public BookingDTO cancelBooking(Long bookingId) {
        if (bookingId == null) throw new IllegalArgumentException("bookingId is required");

        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        authorizationService.requireOwnerOrStaff(booking.getBookedByUserId());
        booking.cancel(LocalDateTime.now());
        return mapper.toDTO(bookingRepository.save(booking));
    }

    /**
     * Checks in a booking by its ID.
     */
    public BookingDTO checkIn(Long bookingId) {
        if (bookingId == null) throw new IllegalArgumentException("bookingId is required");

        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        authorizationService.requireOwnerOrStaff(booking.getBookedByUserId());
        booking.checkIn();
        return mapper.toDTO(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Optional<BookingDTO> locateBookingById(Long bookingId) {
        if (bookingId == null) throw new IllegalArgumentException("bookingId is required");

        return bookingRepository.findById(bookingId)
                .map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> fetchBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> fetchBookingsByRoom(Long roomId) {
        if (roomId == null) throw new IllegalArgumentException("roomId is required");

        return bookingRepository.findByRoomId(roomId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Returns availability for a room within a given time range.
     * Availability is derived from:
     * - Room operational status (enabled/disabled)
     * - Existence of blocking bookings in the requested time range
     */
    @Transactional(readOnly = true)
    public RoomAvailabilityDTO fetchRoomAvailability(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        if (roomId == null) throw new IllegalArgumentException("roomId is required");

        var room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));

        var timeRange = new Booking.TimeRange(startTime, endTime);

        if (room.isDisabled()) {
            return new RoomAvailabilityDTO(roomId, startTime, endTime, "UNAVAILABLE");
        }

        var unavailable = hasOverlappingBlockingBooking(roomId, timeRange);
        return new RoomAvailabilityDTO(roomId, startTime, endTime, unavailable ? "UNAVAILABLE" : "AVAILABLE");
    }

    /**
     * Scheduled task to enforce booking lifecycle rules.
     * Configure with:
     * reservation.lifecycle.delay-ms=60000
     */
    @Scheduled(fixedDelayString = "${reservation.lifecycle.delay-ms:60000}")
    public void enforceLifecycle() {
        var now = LocalDateTime.now();
        moveConfirmedBookingsToCheckInRequired(now);
        markNoShows(now);
        completeCheckedInBookings(now);
    }

    private boolean hasOverlappingBlockingBooking(Long roomId, Booking.TimeRange timeRange) {
        return bookingRepository.existsOverlappingBooking(
                roomId,
                timeRange.startTime(),
                timeRange.endTime(),
                Booking.blockingStatuses()
        );
    }

    private void moveConfirmedBookingsToCheckInRequired(LocalDateTime now) {
        var toUpdate = bookingRepository.findByStatusAndTimeRangeStartTimeLessThanEqual(BookingStatus.CONFIRMED, now);

        toUpdate.forEach(Booking::requireCheckIn);

        if (!toUpdate.isEmpty()) {
            bookingRepository.saveAll(toUpdate);
        }
    }

    private void markNoShows(LocalDateTime now) {
        var toReview = bookingRepository.findByStatusAndTimeRangeStartTimeLessThanEqual(
                BookingStatus.CHECK_IN_REQUIRED,
                now
        );

        var toUpdate = toReview.stream()
                .filter(b -> !now.isBefore(b.getTimeRange().startTime().plusMinutes(CHECK_IN_GRACE_PERIOD_MINUTES)))
                .peek(Booking::markNoShow)
                .toList();

        if (!toUpdate.isEmpty()) {
            bookingRepository.saveAll(toUpdate);
        }
    }

    private void completeCheckedInBookings(LocalDateTime now) {
        var toUpdate = bookingRepository.findByStatusAndTimeRangeEndTimeLessThanEqual(BookingStatus.CHECKED_IN, now);

        toUpdate.forEach(Booking::complete);

        if (!toUpdate.isEmpty()) {
            bookingRepository.saveAll(toUpdate);
        }
    }

    private static String normalizeOwnerUserId(String subject) {
        return (subject == null || subject.isBlank()) ? "anonymous" : subject;
    }
}
