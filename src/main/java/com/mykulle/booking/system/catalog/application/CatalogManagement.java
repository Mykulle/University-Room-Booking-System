package com.mykulle.booking.system.catalog.application;

import com.mykulle.booking.system.catalog.RoomCatalogEvent.*;
import com.mykulle.booking.system.catalog.domain.CatalogRepository;
import com.mykulle.booking.system.catalog.domain.CatalogRoom;
import com.mykulle.booking.system.catalog.domain.CatalogRoom.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class CatalogManagement {

    private final CatalogRepository catalogRepository;
    private final RoomMapper mapper;
    private final ApplicationEventPublisher events;

    /**
     * Adds a new room to the catalog
     */
    public RoomDTO addRoom(String name, String location, RoomType type) {
        var room = new CatalogRoom(new RoomProfile(name, new RoomLocation(location), type));
        var saved = catalogRepository.save(room);
        events.publishEvent(new RoomAddedToCatalog(
                saved.getId(),
                saved.getProfile().name(),
                saved.getProfile().roomLocation().value(),
                saved.getProfile().roomType().name(),
                saved.getOperationalStatus().name()
        ));

        return mapper.toDTO(saved);
    }

    /**
     * Removes a room from the catalog
     */
    public void removeRoom(Long roomId) {
        var room = catalogRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        if(room.isEnabled()) {
            throw new IllegalStateException("Cannot remove an enabled room. Please disable it first.");
        }
        catalogRepository.delete(room);
        events.publishEvent(new RoomRemovedFromCatalog(roomId));
    }

    /**
     * Locates a room by its location
     */
    @Transactional(readOnly = true)
    public Optional<RoomDTO> locateRoom(String roomLocation) {
        return catalogRepository.findByProfileRoomLocationValue(roomLocation)
                .map(mapper::toDTO);
    }


    /**
     * Locates rooms by their type
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> locateRoomsByType(CatalogRoom.RoomType roomType) {
        return catalogRepository.findByProfileRoomType(roomType)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Fetch enabled rooms for booking
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> fetchEnabledRooms() {
        return catalogRepository.findByOperationalStatus(CatalogRoom.OperationalStatus.ENABLED)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Fetch all rooms in the catalog
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> fetchRooms() {
        return catalogRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Enables a room for booking
     */
    public RoomDTO enableRoom(Long roomId) {
        var room = catalogRepository.findById(roomId)
                .map(CatalogRoom::enable)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        var saved = catalogRepository.save(room);
        events.publishEvent(new RoomOperationalStatusChanged(
                saved.getId(),
                saved.getOperationalStatus().name()
        ));
        return mapper.toDTO(saved);
    }

    /**
     * Disables a room for booking
     */
    public RoomDTO disableRoom(Long roomId) {
        var room = catalogRepository.findById(roomId)
                .map(CatalogRoom::disable)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        var saved = catalogRepository.save(room);
        events.publishEvent(new RoomOperationalStatusChanged(
                saved.getId(),
                saved.getOperationalStatus().name()
        ));
        return mapper.toDTO(saved);
    }


}
