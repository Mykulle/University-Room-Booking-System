package com.mykulle.booking.system.reservation.booking.application;

import com.mykulle.booking.system.reservation.booking.domain.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mappings({
            @Mapping(target = "startTime", source = "timeRange.startTime"),
            @Mapping(target = "endTime", source = "timeRange.endTime"),
            @Mapping(target = "status", source = "status")
    })
    BookingDTO toDTO(Booking booking);

    Booking toEntity(BookingDTO bookingDTO);
}
