package com.mykulle.booking.system.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mykulle.booking.system.booking.Booking.BookingStatus;
import com.mykulle.booking.system.rooms.RoomDTO;
import com.mykulle.booking.system.rooms.RoomManagement;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingManagement bookings;

    @Mock
    private RoomManagement rooms;

    @InjectMocks
    private BookingController controller;

    private static final String ROOM_LOCATION = "LIB-03-12";
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2026, 2, 3, 10, 0);
        end = start.plusHours(1);
    }

    @Test
    void createBooking_returnsCreatedBooking() {
        var dto = new BookingDTO(1L, ROOM_LOCATION, start, end, LocalDate.now(), BookingStatus.CONFIRMED);
        when(bookings.createBooking(eq(ROOM_LOCATION), eq(start), eq(end))).thenReturn(dto);

        var request = new BookingController.CreateBookingRequest(ROOM_LOCATION, start, end);

        var resp = controller.createBooking(request);

        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(dto);

        verify(bookings).createBooking(ROOM_LOCATION, start, end);
    }

    @Test
    void getBooking_returnsSingleBooking() {
        var dto = new BookingDTO(5L, ROOM_LOCATION, start, end, LocalDate.now(), BookingStatus.CHECKED_IN);
        when(bookings.getBookingById(5L)).thenReturn(dto);

        var resp = controller.getBooking(5L);

        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(dto);

        verify(bookings).getBookingById(5L);
    }

    @Test
    void listAvailableRooms_returnsInactiveRooms() {
        when(rooms.listRoomByStatus("INACTIVE")).thenReturn(
                List.of(new RoomDTO(2L, "Lecture Hall", ROOM_LOCATION, null)));

        var resp = controller.listAvailableRooms();

        assertThat(resp.getBody()).isNotEmpty();
        assertThat(resp.getBody().get(0).roomLocation()).isEqualTo(ROOM_LOCATION);

        verify(rooms).listRoomByStatus("INACTIVE");
    }
}
