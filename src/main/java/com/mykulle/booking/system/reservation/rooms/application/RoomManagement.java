package com.mykulle.booking.system.reservation.rooms.application;

import com.mykulle.booking.system.catalog.RoomCatalogEvent;
import com.mykulle.booking.system.reservation.rooms.domain.Room;
import com.mykulle.booking.system.reservation.rooms.domain.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class RoomManagement {

    private final RoomRepository rooms;

    @ApplicationModuleListener
    public void on(RoomCatalogEvent.RoomAddedToCatalog e) {
        var profile = new Room.RoomProfile(
                e.name(),
                new Room.RoomLocation(e.roomLocation()),
                e.type()
        );

        var room = rooms.findById(e.roomId())
                .map(existing -> {
                    existing.update(profile, e.operationalStatus());
                    return existing;
                })
                .orElseGet(() -> new Room(e.roomId(), profile, e.operationalStatus()));

        rooms.save(room);
    }

    @ApplicationModuleListener
    public void on(RoomCatalogEvent.RoomOperationalStatusChanged e) {
        rooms.findById(e.roomId()).ifPresent(room -> {
            room.update(room.getProfile(), e.operationalStatus());
            rooms.save(room);
        });
    }

    @ApplicationModuleListener
    public void on(RoomCatalogEvent.RoomRemovedFromCatalog e) {
        rooms.deleteById(e.roomId());
    }
}
