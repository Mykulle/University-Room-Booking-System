package com.mykulle.booking.system.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mykulle.booking.system.booking.Booking.BookingStatus;
import com.mykulle.booking.system.rooms.RoomDTO;
import com.mykulle.booking.system.rooms.RoomManagement;

@ExtendWith(MockitoExtension.class)
class BookingManagementTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private RoomManagement roomManagement;

    @InjectMocks
    private BookingManagement bookingManagement;

    private static final String ROOM_LOCATION = "LIB-03-12";
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        // Use a date relative to 'now' so tests are not time-dependent
        start = LocalDate.now().plusDays(1).atTime(10, 0);
        end = start.plusHours(1);
    }

    @Test
    void createBooking_persistsWhenSlotFree() {
        var booking = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start, end));
        var expectedDto = new BookingDTO(1L, ROOM_LOCATION, start, end, booking.getBookingDate(), BookingStatus.CONFIRMED);

        when(bookingRepository.findConflictingBookings(eq(ROOM_LOCATION), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toDTO(booking)).thenReturn(expectedDto);

        var result = bookingManagement.createBooking(ROOM_LOCATION, start, end);

        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).findConflictingBookings(ROOM_LOCATION, start.minusMinutes(30), end.plusMinutes(30));
        verify(bookingRepository).save(any(Booking.class));
        verifyNoInteractions(roomManagement);
    }

    @Test
    void createBooking_rejectsDurationOtherThanOneHour() {
        var invalidEnd = start.plusMinutes(30);

        assertThatThrownBy(() -> bookingManagement.createBooking(ROOM_LOCATION, start, invalidEnd))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("exactly 1 hour");
    }

    @Test
    void createBooking_rejectsConflictingSlot() {
        var existing = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start.minusMinutes(15), end.plusMinutes(15)));
        when(bookingRepository.findConflictingBookings(eq(ROOM_LOCATION), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> bookingManagement.createBooking(ROOM_LOCATION, start, end))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conflicts");
    }

    @Test
    void checkInBooking_marksBookingAndRoomActive() {
        var booking = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start, end));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(roomManagement.isRoomActive(ROOM_LOCATION)).thenReturn(false);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDTO(booking)).thenReturn(
                new BookingDTO(1L, ROOM_LOCATION, start, end, LocalDate.now(), BookingStatus.CHECKED_IN));

        var dto = bookingManagement.checkInBooking(1L);

        assertThat(booking.isCheckedIn()).isTrue();
        assertThat(dto.status()).isEqualTo(BookingStatus.CHECKED_IN);
        verify(roomManagement).activateRoomByLocation(ROOM_LOCATION);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_deactivatesRoomWhenActive() {
        var booking = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start, end));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(roomManagement.isRoomActive(ROOM_LOCATION)).thenReturn(true);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDTO(booking)).thenReturn(
                new BookingDTO(1L, ROOM_LOCATION, start, end, LocalDate.now(), BookingStatus.CANCELED));

        var dto = bookingManagement.cancelBooking(1L);

        assertThat(booking.isCanceled()).isTrue();
        assertThat(dto.status()).isEqualTo(BookingStatus.CANCELED);
        verify(roomManagement).deactivateRoomByLocation(ROOM_LOCATION);
    }

    @Test
    void listDailyAvailability_marksFutureSlotsAvailableWhenNoBookings() {
        var futureDate = LocalDate.now().plusDays(2);
        when(bookingRepository.findBookingsBetween(any(), any())).thenReturn(List.of());
        when(roomManagement.listAllRooms()).thenReturn(
                List.of(new RoomDTO(10L, "Conference A", ROOM_LOCATION, null)));

        var availability = bookingManagement.listDailyAvailability(futureDate);

        assertThat(availability).hasSize(1);
        var roomAvailability = availability.getFirst();
        assertThat(roomAvailability.roomLocation()).isEqualTo(ROOM_LOCATION);
        assertThat(roomAvailability.slots()).hasSize(10);
        assertThat(roomAvailability.slots()).allMatch(RoomAvailabilityDTO.SlotDTO::available);
    }

    @Test
    void markNoShowBookings_updatesStatusAndFreesRooms() {
        var booking = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start.minusHours(3), end.minusHours(3)));
        when(bookingRepository.findNoShowBookings(any())).thenReturn(List.of(booking));
        when(roomManagement.isRoomActive(ROOM_LOCATION)).thenReturn(true);

        bookingManagement.markNoShowBookings();

        assertThat(booking.isNoShow()).isTrue();
        verify(roomManagement).deactivateRoomByLocation(ROOM_LOCATION);
        verify(bookingRepository).saveAll(List.of(booking));
    }

    @Test
    void freeRoomsForEndedBookings_marksConfirmedAsNoShowAndSaves() {
        var ended = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start.minusHours(2), end.minusHours(2)));
        when(bookingRepository.findEndedBookings(any())).thenReturn(List.of(ended));
        when(roomManagement.isRoomActive(ROOM_LOCATION)).thenReturn(true);

        bookingManagement.freeRoomsForEndedBookings();

        assertThat(ended.isNoShow()).isTrue();
        verify(roomManagement).deactivateRoomByLocation(ROOM_LOCATION);
        verify(bookingRepository).saveAll(List.of(ended));
    }

    @Test
    void createBooking_rejectsWhenStartInPast() {
        var pastStart = LocalDateTime.now().minusHours(2);
        var pastEnd = pastStart.plusHours(1);

        assertThatThrownBy(() -> bookingManagement.createBooking(ROOM_LOCATION, pastStart, pastEnd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("starts in the past");
    }

    @Test
    void checkInBooking_throwsWhenRoomAlreadyActive() {
        var booking = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start, end));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(roomManagement.isRoomActive(ROOM_LOCATION)).thenReturn(true);

        assertThatThrownBy(() -> bookingManagement.checkInBooking(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("room is already active");
    }

    @Test
    void getBookingById_throwsWhenNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingManagement.getBookingById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void cancelBooking_doesNotDeactivateWhenRoomInactive() {
        var booking = new Booking(ROOM_LOCATION, new Booking.TimeSlot(start, end));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(roomManagement.isRoomActive(ROOM_LOCATION)).thenReturn(false);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDTO(booking)).thenReturn(
                new BookingDTO(1L, ROOM_LOCATION, start, end, LocalDate.now(), BookingStatus.CANCELED));

        var dto = bookingManagement.cancelBooking(1L);

        assertThat(booking.isCanceled()).isTrue();
        assertThat(dto.status()).isEqualTo(BookingStatus.CANCELED);
        verify(roomManagement, never()).deactivateRoomByLocation(ROOM_LOCATION);
    }
}

