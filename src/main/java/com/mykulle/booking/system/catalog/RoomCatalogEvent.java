package com.mykulle.booking.system.catalog;


import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public interface RoomCatalogEvent {

    record RoomOperationalStatusChanged(Long roomId, String operationalStatus) implements RoomCatalogEvent{}
    record RoomAddedToCatalog(Long roomId, String name, String roomLocation, String type, String operationalStatus) implements RoomCatalogEvent{}
    record RoomRemovedFromCatalog(Long roomId) implements RoomCatalogEvent {}
}

