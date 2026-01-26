package com.mykulle.booking.system.rooms;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
class RoomController {

    private final RoomManagement rooms;

    @PostMapping
    ResponseEntity<RoomDTO> addRoom(@Valid @RequestBody  AddRoomRequest request) {
        var roomDTO = rooms.addRoom(request.name(), request.roomLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body(roomDTO);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> removeRoom(@PathVariable("id") Long id) {
        rooms.removeRoom(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    ResponseEntity<List<RoomDTO>> listAllRooms() {
        var roomDTOs = rooms.listAllRooms();
        return ResponseEntity.ok(roomDTOs);
    }

    @GetMapping(params = "status")
    ResponseEntity<List<RoomDTO>> listAllRoomsByStatus(@RequestParam("status") Room.RoomStatus status) {
        var roomDTOs = rooms.listRoomByStatus(status);
        return ResponseEntity.ok(roomDTOs);
    }

    record AddRoomRequest(
            String name,
            String roomLocation
    ) {}
}
