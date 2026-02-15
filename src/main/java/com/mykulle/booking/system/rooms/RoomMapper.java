package com.mykulle.booking.system.rooms;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface RoomMapper {

    @Mapping(source = "location.value", target = "roomLocation")
    RoomDTO toDTO(Room room);

    Room toEntity(RoomDTO roomDTO);
}
