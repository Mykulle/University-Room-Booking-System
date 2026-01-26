package com.mykulle.booking.system.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.room.roomLocation = :roomLocation 
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        AND b.timeSlot.startTime < :endTime 
        AND b.timeSlot.endTime > :startTime
        """)
    List<Booking> findConflictingBookings(
            @Param("roomLocation") String roomLocation,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
