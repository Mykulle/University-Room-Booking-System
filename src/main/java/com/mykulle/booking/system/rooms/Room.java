package com.mykulle.booking.system.rooms;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "rooms")
class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /* Room Location(e.g. "LIB-03-12") */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "building", column = @Column(name = "building", nullable = false)),
        @AttributeOverride(name = "level",    column = @Column(name = "level", nullable = false)),
        @AttributeOverride(name = "roomCode", column = @Column(name = "room_code", nullable = false, updatable = false))
    })
    private RoomLocation location;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    public boolean isActive() {
        return this.status == RoomStatus.ACTIVE;
    }

    public boolean isInactive() {
        return this.status == RoomStatus.INACTIVE;
    }

    public Room(String name, RoomLocation location) {

        this.name = name;
        this.location = location;
        this.status = RoomStatus.ACTIVE;
    }
    @Embeddable
    record RoomLocation(String building, String level, String roomCode) {
        public RoomLocation {
            if (roomCode == null || roomCode.isBlank()) throw new IllegalArgumentException("Room code is required");
            if (building == null || building.isBlank()) throw new IllegalArgumentException("Building is required");
            if (level == null || level.isBlank()) throw new IllegalArgumentException("Level is required");

        }
    }

    public Room setActive() {
        if(isActive()) {
            throw new IllegalStateException("Room is already active");
        }
        this.status = RoomStatus.ACTIVE;
        return this;
    }

    public Room setInActive() {
        if(isActive()) {
            throw new IllegalStateException("Room is inactive");
        }
        this.status = RoomStatus.INACTIVE;
        return this;
    }

    public enum RoomStatus { ACTIVE, INACTIVE }
}

