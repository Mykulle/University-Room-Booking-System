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
    public RoomDTO addRoom(String name, String roomLocation) {

        // Check if room with the same location already exists
        if(roomRepository.existsByRoomLocation(roomLocation)) {
            throw new IllegalArgumentException("Room already exists at location: " + roomLocation);
        }

        var room = new Room(name, new Room.RoomLocation(roomLocation));
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
                .map(Room ::setInactive)
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

    /* List Rooms by Status by name (keeps booking module decoupled from Room enum)
       Accepts case-insensitive status names like "ACTIVE" or "INACTIVE" */
    @Transactional(readOnly = true)
    public List<RoomDTO> listRoomByStatus(String status) {
        try {
            var enumStatus = Room.RoomStatus.valueOf(status.toUpperCase());
            return listRoomByStatus(enumStatus);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid room status: " + status);
        }
    }

    /* Get room by location */
    @Transactional(readOnly = true)
    public RoomDTO getRoomByLocation(String roomLocation) {
        return roomRepository.findRoomByRoomLocation(roomLocation)
                .map(roomMapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with location: " + roomLocation));
    }

    /* Check if a room is active by location (keeps booking module from touching room internals) */
    @Transactional(readOnly = true)
    public boolean isRoomActive(String roomLocation) {
        return roomRepository.findRoomByRoomLocation(roomLocation)
                .map(Room::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with location: " + roomLocation));
    }

    /* Activate room by location */
    @Transactional
    public RoomDTO activateRoomByLocation(String roomLocation) {
        var room = roomRepository.findRoomByRoomLocation(roomLocation)
                .map(Room::setActive)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with location: " + roomLocation));
        return roomMapper.toDTO(roomRepository.save(room));
    }

    /* Deactivate room by location */
    @Transactional
    public RoomDTO deactivateRoomByLocation(String roomLocation) {
        var room = roomRepository.findRoomByRoomLocation(roomLocation)
                .map(Room::setInactive)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with location: " + roomLocation));
        return roomMapper.toDTO(roomRepository.save(room));
    }

}