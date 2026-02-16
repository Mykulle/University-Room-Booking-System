package com.mykulle.booking.system.catalog;

import com.mykulle.booking.system.catalog.application.CatalogManagement;
import com.mykulle.booking.system.catalog.domain.CatalogRoom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RecordApplicationEvents
class CatalogEventsIT {

    @Autowired
    CatalogManagement catalogManagement;

    @Autowired
    ApplicationEvents events;

    @Test
    void addRoom_publishesRoomAddedEvent() {
        var dto = catalogManagement.addRoom("Focus Room", "LIB-03-12", CatalogRoom.RoomType.STUDY_ROOM);

        var published = events.stream(RoomCatalogEvent.RoomAddedToCatalog.class).toList();

        assertThat(published).isNotEmpty();
        assertThat(published).anySatisfy(e -> {
            assertThat(e.roomId()).isEqualTo(dto.id());
            assertThat(e.roomLocation()).isEqualTo("LIB-03-12");
            assertThat(e.operationalStatus()).isEqualTo("ENABLED");
        });
    }
}

