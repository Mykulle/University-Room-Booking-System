package com.mykulle.booking.system.reservation.booking.application;

import com.mykulle.booking.system.reservation.booking.domain.Booking;
import com.mykulle.booking.system.reservation.booking.domain.BookingRepository;
import com.mykulle.booking.system.reservation.rooms.domain.Room;
import com.mykulle.booking.system.reservation.rooms.domain.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingManagementTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingMapper mapper;

    @InjectMocks
    private BookingManagement bookingManagement;

    @Test
    void createBooking_savesBooking_whenRoomIsAvailable() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 0);
        var end = start.plusHours(2);
        var room = enabledRoom(5L);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(eq(5L), eq(start), eq(end), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0, Booking.class));

        var expected = new BookingDTO(null, 5L, start, end, "CONFIRMED");
        when(mapper.toDTO(any(Booking.class))).thenReturn(expected);

        var result = bookingManagement.createBooking(5L, start, end);

        assertThat(result).isEqualTo(expected);

        var bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        var saved = bookingCaptor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(5L);
        assertThat(saved.getTimeRange().startTime()).isEqualTo(start);
        assertThat(saved.getTimeRange().endTime()).isEqualTo(end);
        assertThat(saved.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void createBooking_throws_whenRoomIsDisabled() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 0);
        var end = start.plusHours(1);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(disabledRoom(5L)));

        assertThatThrownBy(() -> bookingManagement.createBooking(5L, start, end))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled room");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void fetchRoomAvailability_returnsUnavailable_whenRoomIsDisabled() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 0);
        var end = start.plusHours(1);

        when(roomRepository.findById(7L)).thenReturn(Optional.of(disabledRoom(7L)));

        var result = bookingManagement.fetchRoomAvailability(7L, start, end);

        assertThat(result.roomId()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        verify(bookingRepository, never()).existsOverlappingBooking(any(), any(), any(), any());
    }

    @Test
    void enforceLifecycle_appliesExpectedTransitions() {
        var now = LocalDateTime.now();

        var confirmed = new Booking(1L, new Booking.TimeRange(now.minusMinutes(5), now.plusMinutes(55)));

        var checkInRequired = new Booking(1L, new Booking.TimeRange(now.minusMinutes(30), now.plusMinutes(30)));
        checkInRequired.requireCheckIn();

        var checkedIn = new Booking(1L, new Booking.TimeRange(now.minusHours(1), now.minusMinutes(1)));
        checkedIn.requireCheckIn();
        checkedIn.checkIn();

        when(bookingRepository.findByStatusAndTimeRangeStartTimeLessThanEqual(eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(confirmed));
        when(bookingRepository.findByStatusAndTimeRangeStartTimeLessThanEqual(eq(Booking.BookingStatus.CHECK_IN_REQUIRED), any(LocalDateTime.class)))
                .thenReturn(List.of(checkInRequired));
        when(bookingRepository.findByStatusAndTimeRangeEndTimeLessThanEqual(eq(Booking.BookingStatus.CHECKED_IN), any(LocalDateTime.class)))
                .thenReturn(List.of(checkedIn));

        bookingManagement.enforceLifecycle();

        assertThat(confirmed.getStatus()).isEqualTo(Booking.BookingStatus.CHECK_IN_REQUIRED);
        assertThat(checkInRequired.getStatus()).isEqualTo(Booking.BookingStatus.NO_SHOW);
        assertThat(checkedIn.getStatus()).isEqualTo(Booking.BookingStatus.COMPLETED);

        verify(bookingRepository).saveAll(List.of(confirmed));
        verify(bookingRepository).saveAll(List.of(checkInRequired));
        verify(bookingRepository).saveAll(List.of(checkedIn));
    }

    private static Room enabledRoom(Long roomId) {
        return new Room(
                roomId,
                new Room.RoomProfile("Focus Room", new Room.RoomLocation("LIB-03-12"), "STUDY_ROOM"),
                "ENABLED"
        );
    }

    private static Room disabledRoom(Long roomId) {
        return new Room(
                roomId,
                new Room.RoomProfile("Focus Room", new Room.RoomLocation("LIB-03-12"), "STUDY_ROOM"),
                "DISABLED"
        );
    }
}

