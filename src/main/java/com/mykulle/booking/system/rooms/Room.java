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

    @Column(nullable = false)
    private String name;

    /* Room Location(e.g. "LIB-03-12") */
    @Embedded
    private RoomLocation location;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    public boolean isActive() {
        return this.status == RoomStatus.ACTIVE;
    }

    public boolean isInactive() {
        return this.status == RoomStatus.INACTIVE;
    }

    public Room(String name, RoomLocation location) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name is required");
        }
        if (location == null) {
            throw new IllegalArgumentException("Room location is required");
        }

        this.name = name;
        this.location = location;
        this.status = RoomStatus.ACTIVE;
    }

    public Room setActive() {
        if(isActive()) {
            throw new IllegalStateException("Room is already active");
        }
        this.status = RoomStatus.ACTIVE;
        return this;
    }

    public Room setInactive() {
        if(isInactive()) {
            throw new IllegalStateException("Room is already inactive");
        }
        this.status = RoomStatus.INACTIVE;
        return this;
    }

    @Embeddable
    record RoomLocation(@Column(name = "room_location", nullable = false, unique = true) String value) {

        private static final String LOCATION_PATTERN = "^[A-Z]{2,10}-\\d{2}-\\d{2,4}$";

        public RoomLocation {

            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Room location is required");
            }

            if (!value.matches(LOCATION_PATTERN)) {
                throw new IllegalArgumentException(
                        "Invalid room location format. Expected format: BUILDING-LEVEL-ROOMCODE (e.g., LIB-03-12)"
                );
            }
        }
    }

    public enum RoomStatus { ACTIVE, INACTIVE }
}

