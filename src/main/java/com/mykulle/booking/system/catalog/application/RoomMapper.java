package com.mykulle.booking.system.catalog.application;

import com.mykulle.booking.system.catalog.domain.CatalogRoom;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {

    @Mappings({
            @Mapping(target = "name", source = "profile.name"),
            @Mapping(target = "roomLocation", source = "profile.roomLocation.value"),
            @Mapping(target = "type", source = "profile.roomType"),
            @Mapping(target = "status", source = "operationalStatus")
    })
    RoomDTO toDTO(CatalogRoom room);

    CatalogRoom toEntity(RoomDTO roomDTO);
}