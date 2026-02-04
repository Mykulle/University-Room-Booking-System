package com.mykulle.booking.system.rooms;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomManagementTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomManagement roomManagement;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("Lecture Hall", new Room.RoomLocation("LIB-03-12"));
    }

    @Test
    void addRoom_persistsAndReturnsDTO() {
        var dto = new RoomDTO(1L, "Lecture Hall", "LIB-03-12", Room.RoomStatus.INACTIVE);
        when(roomRepository.existsByRoomLocation("LIB-03-12")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDTO(room)).thenReturn(dto);

        var result = roomManagement.addRoom("Lecture Hall", "LIB-03-12");

        assertThat(result).isEqualTo(dto);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void addRoom_throwsWhenRoomAlreadyExists() {
        when(roomRepository.existsByRoomLocation("LIB-03-12")).thenReturn(true);

        assertThatThrownBy(() -> roomManagement.addRoom("Lecture Hall", "LIB-03-12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void removeRoom_deletesWhenInactive() {
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room));

        roomManagement.removeRoom(2L);

        verify(roomRepository).delete(room);
    }

    @Test
    void removeRoom_throwsWhenRoomActive() {
        var activeRoom = new Room("Conf", new Room.RoomLocation("LIB-03-13"));
        activeRoom.setActive();
        when(roomRepository.findById(3L)).thenReturn(Optional.of(activeRoom));

        assertThatThrownBy(() -> roomManagement.removeRoom(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove an active room");
    }

    @Test
    void activateRoom_setsActiveAndReturnsDTO() {
        when(roomRepository.findById(4L)).thenReturn(Optional.of(room));
        var dto = new RoomDTO(4L, "Lecture Hall", "LIB-03-12", Room.RoomStatus.ACTIVE);
        when(roomMapper.toDTO(room)).thenReturn(dto);

        var result = roomManagement.activateRoom(4L);

        assertThat(result).isEqualTo(dto);
        assertThat(room.isActive()).isTrue();
    }

    @Test
    void deactivateRoom_setsInactiveAndReturnsDTO() {
        var r = new Room("Rm", new Room.RoomLocation("LIB-03-14"));
        r.setActive();
        when(roomRepository.findById(5L)).thenReturn(Optional.of(r));
        var dto = new RoomDTO(5L, "Rm", "LIB-03-14", Room.RoomStatus.INACTIVE);
        when(roomMapper.toDTO(r)).thenReturn(dto);

        var result = roomManagement.deactivateRoom(5L);

        assertThat(result).isEqualTo(dto);
        assertThat(r.isInactive()).isTrue();
    }

    @Test
    void listRoomByStatus_stringAcceptsCaseInsensitiveAndThrowsOnInvalid() {
        when(roomRepository.findAllByStatus(Room.RoomStatus.INACTIVE)).thenReturn(List.of(room));
        var dto = new RoomDTO(1L, "Lecture Hall", "LIB-03-12", Room.RoomStatus.INACTIVE);
        when(roomMapper.toDTO(room)).thenReturn(dto);

        var result = roomManagement.listRoomByStatus("inactive");
        assertThat(result).hasSize(1);

        assertThatThrownBy(() -> roomManagement.listRoomByStatus("bogus"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getRoomByLocation_returnsWhenFound_elseThrows() {
        var dto = new RoomDTO(1L, "Lecture Hall", "LIB-03-12", Room.RoomStatus.INACTIVE);
        when(roomRepository.findRoomByRoomLocation("LIB-03-12")).thenReturn(Optional.of(room));
        when(roomMapper.toDTO(room)).thenReturn(dto);

        var result = roomManagement.getRoomByLocation("LIB-03-12");

        assertThat(result).isEqualTo(dto);

        when(roomRepository.findRoomByRoomLocation("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomManagement.getRoomByLocation("NOPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isRoomActive_looksUpByLocation() {
        when(roomRepository.findRoomByRoomLocation("LIB-03-12")).thenReturn(Optional.of(room));
        var result = roomManagement.isRoomActive("LIB-03-12");
        assertThat(result).isFalse();

        when(roomRepository.findRoomByRoomLocation("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomManagement.isRoomActive("NOPE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activateAndDeactivateByLocation_changesStatusAndSaves() {
        var r = new Room("Rm", new Room.RoomLocation("LIB-03-14"));
        when(roomRepository.findRoomByRoomLocation("LIB-03-14")).thenReturn(Optional.of(r));
        when(roomRepository.save(r)).thenReturn(r);
        var activeDto = new RoomDTO(10L, "Rm", "LIB-03-14", Room.RoomStatus.ACTIVE);
        when(roomMapper.toDTO(r)).thenReturn(activeDto);

        var after = roomManagement.activateRoomByLocation("LIB-03-14");
        assertThat(after).isEqualTo(activeDto);
        assertThat(r.isActive()).isTrue();

        // deactivate
        var inactiveDto = new RoomDTO(11L, "Rm", "LIB-03-14", Room.RoomStatus.INACTIVE);
        when(roomRepository.findRoomByRoomLocation("LIB-03-14")).thenReturn(Optional.of(r));
        when(roomRepository.save(r)).thenReturn(r);
        when(roomMapper.toDTO(r)).thenReturn(inactiveDto);

        var after2 = roomManagement.deactivateRoomByLocation("LIB-03-14");
        assertThat(after2).isEqualTo(inactiveDto);
        assertThat(r.isInactive()).isTrue();
    }

    @Test
    void listAllRooms_returnsMappedDTOs() {
        var dto = new RoomDTO(1L, "Lecture Hall", "LIB-03-12", Room.RoomStatus.INACTIVE);
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomMapper.toDTO(room)).thenReturn(dto);

        var list = roomManagement.listAllRooms();
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isEqualTo(dto);
    }
}
