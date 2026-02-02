package com.mykulle.booking.system.booking;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mykulle.booking.system.rooms.RoomManagement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BookingManagement {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final RoomManagement rooms;

    private static final long REQUIRED_DURATION_HOURS = 1;
    private static final long BUFFER_MINUTES = 30;
    private static final long CHECKIN_GRACE_MINUTES = 15;

    public BookingDTO createBooking(String roomLocation, LocalDateTime startTime, LocalDateTime endTime) {
        validateBookingRequest(roomLocation, startTime, endTime);

        validateBookingNotInPast(startTime, endTime);

        var booking = new Booking(roomLocation, new Booking.TimeSlot(startTime, endTime));
        var saved = bookingRepository.save(booking);
        log.info("Created booking {} for room {} from {} to {}", saved.getId(), roomLocation, startTime, endTime);
        return bookingMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        return bookingMapper.toDTO(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(bookingMapper::toDTO)
                .toList();
    }

    public BookingDTO checkInBooking(Long id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        booking.checkIn();

        var roomLocation = booking.getRoom().roomLocation();
        if (rooms.isRoomActive(roomLocation)) {
            throw new IllegalStateException("Cannot check-in: room is already active");
        }
        rooms.activateRoomByLocation(roomLocation);
        log.info("Booking {} checked-in and room {} set to ACTIVE", id, roomLocation);

        return bookingMapper.toDTO(bookingRepository.save(booking));
    }

    public BookingDTO cancelBooking(Long id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        booking.cancel();

        var roomLocation = booking.getRoom().roomLocation();
        deactivateIfActive(roomLocation);
        log.info("Booking {} canceled and room {} set to INACTIVE if active", id, roomLocation);

        return bookingMapper.toDTO(bookingRepository.save(booking));
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void markNoShowBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CHECKIN_GRACE_MINUTES);
        List<Booking> noShowBookings = bookingRepository.findNoShowBookings(threshold);

        if(!noShowBookings.isEmpty()) {
            for (Booking booking : noShowBookings) {
                booking.markAsNoShow();
                var roomLocation = booking.getRoom().roomLocation();
                deactivateIfActive(roomLocation);
            }
            bookingRepository.saveAll(noShowBookings);
            log.info("Marked {} bookings as NO-SHOW", noShowBookings.size());
        }
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void freeRoomsForEndedBookings() {
        var now = LocalDateTime.now();
        List<Booking> ended = bookingRepository.findEndedBookings(now);
        if (!ended.isEmpty()) {
            boolean statusChanged = false;
            for (Booking booking : ended) {
                var roomLocation = booking.getRoom().roomLocation();
                try {
                    if (booking.isConfirmed()) {
                        booking.markAsNoShow();
                        log.info("Booking {} marked as NO_SHOW due to end time passing", booking.getId());
                        statusChanged = true;
                    }

                    deactivateIfActive(roomLocation);
                } catch (Exception ex) {
                    log.warn("Failed to process ended booking {} / deactivate room {}: {}", booking.getId(), roomLocation, ex.getMessage());
                }
            }
            if (statusChanged) {
                bookingRepository.saveAll(ended);
            }
            log.info("Processed {} ended bookings to free rooms", ended.size());
        }
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> listDailyAvailability(LocalDate date) {
        var day = (date == null) ? LocalDate.now() : date;
        LocalDateTime startOfDay = day.atTime(8, 0);
        LocalDateTime endOfDay = day.atTime(18, 0);

        var bookings = bookingRepository.findBookingsBetween(startOfDay, endOfDay);
        Map<String, List<Booking>> bookingsByRoom = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getRoom().roomLocation()));

        var now = LocalDateTime.now();

        return rooms.listAllRooms()
                .stream()
                .map(r -> {
                    String loc = r.roomLocation();
                    List<RoomAvailabilityDTO.SlotDTO> slots = java.util.stream.IntStream.range(8, 18)
                            .mapToObj(hour -> {
                                LocalDateTime slotStart = day.atTime(hour, 0);
                                LocalDateTime slotEnd = slotStart.plusHours(1);

                                boolean occupied = bookingsByRoom.getOrDefault(loc, java.util.Collections.emptyList())
                                        .stream()
                                        .anyMatch(b -> b.getTimeSlot().startTime().isBefore(slotEnd)
                                                && b.getTimeSlot().endTime().isAfter(slotStart));

                                boolean inPast = day.equals(now.toLocalDate()) && slotStart.isBefore(now);

                                boolean available = !occupied && !inPast;

                                return new RoomAvailabilityDTO.SlotDTO(slotStart.toLocalTime(), slotEnd.toLocalTime(), available);
                            })
                            .toList();
                    return new RoomAvailabilityDTO(loc, r.name(), slots);
                })
                .toList();
    }

    /* Validation Methods */
    private void validateBookingRequest(String roomLocation, LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeSlotDuration(startTime, endTime);
        validateNoBookingConflicts(roomLocation, startTime, endTime);
    }

    /* Ensure bookings are not created for time ranges that are already in the past */
    private void validateBookingNotInPast(LocalDateTime startTime, LocalDateTime endTime) {
        var now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("Cannot create booking that starts in the past");
        }
        if (endTime.isBefore(now) || endTime.isEqual(now)) {
            throw new IllegalArgumentException("Cannot create booking that ends in the past or at the current time");
        }
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

    private void deactivateIfActive(String roomLocation) {
        try {
            if (rooms.isRoomActive(roomLocation)) {
                rooms.deactivateRoomByLocation(roomLocation);
            }
        } catch (Exception ex) {
            log.warn("Could not deactivate room {}: {}", roomLocation, ex.getMessage());
        }
    }
}
