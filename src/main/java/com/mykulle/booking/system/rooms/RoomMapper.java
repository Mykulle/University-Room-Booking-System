package com.mykulle.booking.system.rooms;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface RoomMapper {
    @Mapping(target = "building", source = "location.building")
    @Mapping(target = "level", source = "location.level")
    @Mapping(target = "roomCode", source = "location.roomCode")
    RoomDTO toDTO(Room room);

    @Mapping(target = "location", expression = "java(new Room.RoomLocation(roomDTO.building(), roomDTO.level(), roomDTO.roomCode()))")
    Room toEntity(RoomDTO roomDTO);
}
