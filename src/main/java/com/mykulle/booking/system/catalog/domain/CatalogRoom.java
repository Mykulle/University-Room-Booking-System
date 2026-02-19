package com.mykulle.booking.system.catalog.domain;


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
@Table(name="catalog_room")
public class CatalogRoom {

    @Identity
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Embedded
    private RoomProfile profile;

    @Version
    private Long version;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationalStatus operationalStatus;

    public CatalogRoom(RoomProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Room profile is required");
        this.profile = profile;
        this.operationalStatus = OperationalStatus.ENABLED;
    }

    public boolean isEnabled() {
        return this.operationalStatus == OperationalStatus.ENABLED;
    }

    public boolean isDisabled() {
        return this.operationalStatus == OperationalStatus.DISABLED;
    }

    @ValueObject
    @Embeddable
    public record RoomProfile(@Column(nullable = false) String name,
                              @Embedded RoomLocation roomLocation,
                              @Enumerated(EnumType.STRING) @Column(nullable = false) RoomType roomType) {

        public RoomProfile {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Room name is required");
            if (roomLocation == null) throw new IllegalArgumentException("Room location is required");
            if (roomType == null) throw new IllegalArgumentException("Room type is required");
        }
    }

    @ValueObject
    @Embeddable
    public record RoomLocation(@Column(name = "room_location", nullable = false, unique = true) String value) {

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

    public CatalogRoom enable() {
        if(isEnabled()) {
            throw new IllegalStateException("Room is already enabled");
        }
        this.operationalStatus = OperationalStatus.ENABLED;
        return this;
    }

    public CatalogRoom disable() {
        if(isDisabled()) {
            throw new IllegalStateException("Room is already disabled");
        }
        this.operationalStatus = OperationalStatus.DISABLED;
        return this;
    }

    public void updateProfile(RoomProfile newProfile) {
        if (newProfile == null) throw new IllegalArgumentException("Room profile is required");
        this.profile = newProfile;
    }

    public enum RoomType { STUDY_ROOM, MEETING_ROOM, CONFERENCE_ROOM }

    public enum OperationalStatus { ENABLED, DISABLED }
}
