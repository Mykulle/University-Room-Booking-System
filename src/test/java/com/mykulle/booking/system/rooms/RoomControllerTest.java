package com.mykulle.booking.system.rooms;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomManagement rooms;

    @InjectMocks
    private RoomController controller;

    @Test
    void addRoom_returnsCreated() {
        var dto = new RoomDTO(1L, "Lecture Hall", "LIB-03-12", null);
        when(rooms.addRoom(eq("Lecture Hall"), eq("LIB-03-12"))).thenReturn(dto);

        var request = new RoomController.AddRoomRequest("Lecture Hall", "LIB-03-12");
        var resp = controller.addRoom(request);

        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(dto);

        verify(rooms).addRoom("Lecture Hall", "LIB-03-12");
    }

    @Test
    void removeRoom_callsRemoveAndReturnsOk() {
        var resp = controller.removeRoom(5L);
        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);

        verify(rooms).removeRoom(5L);
    }

    @Test
    void listAllRooms_returnsList() {
        when(rooms.listAllRooms()).thenReturn(List.of(new RoomDTO(2L, "Rm", "LIB-03-12", null)));

        var resp = controller.listAllRooms();
        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(resp.getBody().get(0).roomLocation()).isEqualTo("LIB-03-12");

        verify(rooms).listAllRooms();
    }

    @Test
    void listAllRoomsByStatus_returnsFiltered() {
        when(rooms.listRoomByStatus(Room.RoomStatus.INACTIVE)).thenReturn(List.of(new RoomDTO(2L, "Rm", "LIB-03-12", null)));

        var resp = controller.listAllRoomsByStatus(Room.RoomStatus.INACTIVE);
        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(resp.getBody().get(0).roomLocation()).isEqualTo("LIB-03-12");

        verify(rooms).listRoomByStatus(Room.RoomStatus.INACTIVE);
    }

}
