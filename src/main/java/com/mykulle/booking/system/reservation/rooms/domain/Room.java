package com.mykulle.booking.system.reservation.rooms.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;

@AggregateRoot
@Entity
@Getter
@NoArgsConstructor
@Table(name = "reservation_room")
public class Room {

    @Identity
    @Id
    private Long roomId;

    @Embedded

    private RoomProfile profile;

    @Column(nullable = false)
    private String operationalStatus;

    public Room(Long roomId, RoomProfile profile, String operationalStatus) {
        if (roomId == null) throw new IllegalArgumentException("roomId is required");
        if (profile == null) throw new IllegalArgumentException("profile is required");
        if (operationalStatus == null || operationalStatus.isBlank())
            throw new IllegalArgumentException("operationalStatus is required");

        this.roomId = roomId;
        this.profile = profile;
        this.operationalStatus = operationalStatus;
    }

    public void update(RoomProfile profile, String operationalStatus) {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        if (operationalStatus == null || operationalStatus.isBlank())
            throw new IllegalArgumentException("operationalStatus is required");

        this.profile = profile;
        this.operationalStatus = operationalStatus;
    }

    public boolean isEnabled() {return "ENABLED".equalsIgnoreCase(this.operationalStatus);}

    public boolean isDisabled() {return "DISABLED".equalsIgnoreCase(this.operationalStatus);}

    @ValueObject
    @Embeddable
    public record RoomProfile(
            @Column(nullable = false) String name,
            @Embedded RoomLocation roomLocation,
            @Column(name = "room_type", nullable = false) String roomType
    ) {
        public RoomProfile {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
            if (roomLocation == null) throw new IllegalArgumentException("roomLocation is required");
            if (roomType == null || roomType.isBlank()) throw new IllegalArgumentException("roomType is required");
        }
    }

    @ValueObject
    @Embeddable
    public record RoomLocation(@Column(name = "room_location", nullable = false) String value) {
        public RoomLocation {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("roomLocation value is required");
        }
    }
}
