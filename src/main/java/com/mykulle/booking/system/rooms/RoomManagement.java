package com.mykulle.booking.system.rooms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class RoomManagement {
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    /* Add a new room to the system */
    public RoomDTO addRoom(String name, String building, String level, String roomCode) {

        // Check if room with the same location already exists
        if(roomRepository.existsByLocationBuildingAndLocationLevelAndLocationRoomCode(building, level, roomCode)) {
            throw new IllegalArgumentException("Room already exists at location: " + building + "-" + level + "-" + roomCode);
        }

        var room = new Room(name, new Room.RoomLocation(building, level, roomCode));
        return roomMapper.toDTO(roomRepository.save(room));

    }

    /* Remove a room from the system */
    public void removeRoom(Long roomId) {
        var room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        if(room.isActive()) throw new IllegalStateException("Cannot remove an active room");
        roomRepository.delete(room);
    }

    /* Set room as active so it cannot be booked */
    public RoomDTO activateRoom(Long roomId) {
        var room = roomRepository.findById(roomId)
                .map(Room ::setActive)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        return roomMapper.toDTO(room);
    }

    /* Set room as inactive so it can be booked */
    public RoomDTO deactivateRoom(Long roomId) {
        var room = roomRepository.findById(roomId)
                .map(Room ::setInActive)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + roomId));
        return roomMapper.toDTO(room);
    }

    /* List all rooms */
    @Transactional(readOnly = true)
    public List<RoomDTO> listAllRooms() {
        return roomRepository.findAll()
                .stream()
                .map(roomMapper::toDTO)
                .toList();


    }

    /* List Rooms by Status */
    @Transactional(readOnly = true)
    public List<RoomDTO> listRoomByStatus(Room.RoomStatus status) {
        return roomRepository.findAllByStatus(status)
                .stream()
                .map(roomMapper::toDTO)
                .toList();
    }

}