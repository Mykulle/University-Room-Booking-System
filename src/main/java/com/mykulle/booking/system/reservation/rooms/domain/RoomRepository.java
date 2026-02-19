package com.mykulle.booking.system.reservation.rooms.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from Room r
            where r.roomId = :roomId
            """)
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);
}
