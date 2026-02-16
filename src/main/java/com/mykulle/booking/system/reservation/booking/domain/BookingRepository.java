package com.mykulle.booking.system.reservation.booking.domain;

import com.mykulle.booking.system.reservation.booking.domain.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select (count(b) > 0)
            from Booking b
            where b.roomId = :roomId
              and b.status in :statuses
              and b.timeRange.startTime < :endTime
              and b.timeRange.endTime > :startTime
            """)
    boolean existsOverlappingBooking(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    List<Booking> findByStatusAndTimeRangeStartTimeLessThanEqual(BookingStatus status, LocalDateTime startTime);

    List<Booking> findByStatusAndTimeRangeEndTimeLessThanEqual(BookingStatus status, LocalDateTime endTime);

    List<Booking> findByRoomId(Long roomId);
}
