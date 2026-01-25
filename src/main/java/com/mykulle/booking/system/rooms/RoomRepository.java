package com.mykulle.booking.system.rooms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByStatus(Room.RoomStatus status);

    Optional<Room> findRoomByName(String name);

    boolean existsByLocationBuildingAndLocationLevelAndLocationRoomCode(String building, String level, String roomCode);

}