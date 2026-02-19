package com.mykulle.booking.system.catalog.ui;

import com.mykulle.booking.system.catalog.application.CatalogManagement;
import com.mykulle.booking.system.catalog.application.RoomDTO;
import com.mykulle.booking.system.catalog.domain.CatalogRoom;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class CatalogController {

    private final CatalogManagement catalogManagement;

    @PostMapping
    ResponseEntity<RoomDTO> addRoom(@Valid @RequestBody addRoomRequest request) {
        var roomDTO = catalogManagement.addRoom(request.name(), request.roomLocation(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(roomDTO);
    }

    @DeleteMapping("/{roomId}")
    ResponseEntity<Void> removeRoom(@PathVariable Long roomId) {
        catalogManagement.removeRoom(roomId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> locateRoomById(@PathVariable Long roomId) {
        return catalogManagement.locateRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/locate")
    public ResponseEntity<RoomDTO> locateRoomByLocation(@RequestParam String roomLocation) {
        return catalogManagement.locateRoom(roomLocation)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<RoomDTO>> fetchEnabledRooms() {
        return ResponseEntity.ok(catalogManagement.fetchEnabledRooms());
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> fetchRooms() {
        return ResponseEntity.ok(catalogManagement.fetchRooms());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<RoomDTO>> locateRoomsByType(@PathVariable CatalogRoom.RoomType type) {
        return ResponseEntity.ok(catalogManagement.locateRoomsByType(type));
    }

    @PutMapping("/{roomId}/enable")
    public ResponseEntity<RoomDTO> enableRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(catalogManagement.enableRoom(roomId));
    }

    @PutMapping("/{roomId}/disable")
    public ResponseEntity<RoomDTO> disableRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(catalogManagement.disableRoom(roomId));
    }


    record addRoomRequest(
        @NotBlank String name,
        @NotBlank String roomLocation,
        @NotNull CatalogRoom.RoomType type
    ) {}
}
