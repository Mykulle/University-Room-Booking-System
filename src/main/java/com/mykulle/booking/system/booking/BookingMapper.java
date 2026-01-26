package com.mykulle.booking.system.booking;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface BookingMapper {

    @Mapping(source = "room.roomLocation", target = "roomLocation")
    @Mapping(source = "timeSlot.startTime", target = "startTime")
    @Mapping(source = "timeSlot.endTime", target = "endTime")
    BookingDTO toDTO(Booking booking);

    Booking toEntity(BookingDTO bookingDTO);
}
