package com.mykulle.booking.system.catalog.application;

import com.mykulle.booking.system.catalog.RoomCatalogEvent;
import com.mykulle.booking.system.catalog.domain.CatalogRepository;
import com.mykulle.booking.system.catalog.domain.CatalogRoom;
import com.mykulle.booking.system.useraccount.api.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogManagementTest {

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private RoomMapper mapper;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private CatalogManagement catalogManagement;

    @Test
    void addRoom_savesRoom_publishesEvent_andReturnsDto() {
        when(catalogRepository.save(any(CatalogRoom.class))).thenAnswer(invocation -> {
            var room = invocation.getArgument(0, CatalogRoom.class);
            ReflectionTestUtils.setField(room, "Id", 10L);
            return room;
        });

        var expected = new RoomDTO(10L, "Focus Room", "LIB-03-12", "STUDY_ROOM", "ENABLED");
        when(mapper.toDTO(any(CatalogRoom.class))).thenReturn(expected);

        var result = catalogManagement.addRoom("Focus Room", "LIB-03-12", CatalogRoom.RoomType.STUDY_ROOM);

        assertThat(result).isEqualTo(expected);
        verify(authorizationService).requireStaff();
        verify(catalogRepository).save(any(CatalogRoom.class));

        var eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()).isInstanceOf(RoomCatalogEvent.RoomAddedToCatalog.class);
        var event = (RoomCatalogEvent.RoomAddedToCatalog) eventCaptor.getValue();
        assertThat(event.roomId()).isEqualTo(10L);
        assertThat(event.name()).isEqualTo("Focus Room");
        assertThat(event.roomLocation()).isEqualTo("LIB-03-12");
        assertThat(event.type()).isEqualTo("STUDY_ROOM");
        assertThat(event.operationalStatus()).isEqualTo("ENABLED");
    }

    @Test
    void removeRoom_throws_whenRoomIsEnabled() {
        var room = new CatalogRoom(new CatalogRoom.RoomProfile(
                "Focus Room",
                new CatalogRoom.RoomLocation("LIB-03-12"),
                CatalogRoom.RoomType.STUDY_ROOM
        ));

        when(catalogRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> catalogManagement.removeRoom(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disable");

        verify(authorizationService).requireStaff();
        verify(catalogRepository, never()).delete(any(CatalogRoom.class));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void removeRoom_deletesRoom_andPublishesEvent_whenRoomIsDisabled() {
        var room = new CatalogRoom(new CatalogRoom.RoomProfile(
                "Focus Room",
                new CatalogRoom.RoomLocation("LIB-03-12"),
                CatalogRoom.RoomType.STUDY_ROOM
        ));
        room.disable();

        when(catalogRepository.findById(22L)).thenReturn(Optional.of(room));

        catalogManagement.removeRoom(22L);

        verify(authorizationService).requireStaff();
        verify(catalogRepository).delete(room);

        var eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()).isInstanceOf(RoomCatalogEvent.RoomRemovedFromCatalog.class);
        var event = (RoomCatalogEvent.RoomRemovedFromCatalog) eventCaptor.getValue();
        assertThat(event.roomId()).isEqualTo(22L);
    }
}
