package com.mykulle.booking.system.rooms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByStatus(Room.RoomStatus status);

    Optional<Room> findRoomByName(String name);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r WHERE r.location.value = :roomLocation")
    boolean existsByRoomLocation(@Param("roomLocation") String roomLocation);

}