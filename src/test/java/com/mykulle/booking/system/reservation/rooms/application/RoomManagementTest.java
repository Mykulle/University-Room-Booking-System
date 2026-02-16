package com.mykulle.booking.system.reservation.rooms.application;

import com.mykulle.booking.system.catalog.RoomCatalogEvent;
import com.mykulle.booking.system.reservation.rooms.domain.Room;
import com.mykulle.booking.system.reservation.rooms.domain.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomManagementTest {

    @Mock
    private RoomRepository rooms;

    @InjectMocks
    private RoomManagement roomManagement;

    @Test
    void onRoomAddedToCatalog_createsRoom_whenMissing() {
        var event = new RoomCatalogEvent.RoomAddedToCatalog(
                11L,
                "Focus Room",
                "LIB-03-12",
                "STUDY_ROOM",
                "ENABLED"
        );

        when(rooms.findById(11L)).thenReturn(Optional.empty());

        roomManagement.on(event);

        var captor = ArgumentCaptor.forClass(Room.class);
        verify(rooms).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(11L);
        assertThat(saved.getProfile().name()).isEqualTo("Focus Room");
        assertThat(saved.getProfile().roomLocation().value()).isEqualTo("LIB-03-12");
        assertThat(saved.getProfile().roomType()).isEqualTo("STUDY_ROOM");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void onRoomAddedToCatalog_updatesRoom_whenExisting() {
        var existing = new Room(
                11L,
                new Room.RoomProfile("Old Room", new Room.RoomLocation("LIB-03-01"), "MEETING_ROOM"),
                "DISABLED"
        );

        when(rooms.findById(11L)).thenReturn(Optional.of(existing));

        roomManagement.on(new RoomCatalogEvent.RoomAddedToCatalog(
                11L,
                "Focus Room",
                "LIB-03-12",
                "STUDY_ROOM",
                "ENABLED"
        ));

        verify(rooms).save(existing);
        assertThat(existing.getProfile().name()).isEqualTo("Focus Room");
        assertThat(existing.getProfile().roomLocation().value()).isEqualTo("LIB-03-12");
        assertThat(existing.getProfile().roomType()).isEqualTo("STUDY_ROOM");
        assertThat(existing.isEnabled()).isTrue();
    }

    @Test
    void onRoomOperationalStatusChanged_updatesStatus_whenRoomExists() {
        var existing = new Room(
                11L,
                new Room.RoomProfile("Focus Room", new Room.RoomLocation("LIB-03-12"), "STUDY_ROOM"),
                "ENABLED"
        );

        when(rooms.findById(11L)).thenReturn(Optional.of(existing));

        roomManagement.on(new RoomCatalogEvent.RoomOperationalStatusChanged(11L, "DISABLED"));

        verify(rooms).save(existing);
        assertThat(existing.isDisabled()).isTrue();
    }

    @Test
    void onRoomRemovedFromCatalog_deletesById() {
        roomManagement.on(new RoomCatalogEvent.RoomRemovedFromCatalog(25L));

        verify(rooms).deleteById(25L);
    }
}
