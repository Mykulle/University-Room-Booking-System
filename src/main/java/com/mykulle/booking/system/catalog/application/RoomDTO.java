package com.mykulle.booking.system.catalog.application;



public record RoomDTO(
        Long id,
        String name,
        String roomLocation,
        String type,
        String status
) {}


