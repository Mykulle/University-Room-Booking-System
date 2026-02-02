package com.mykulle.booking.system.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "bookings")
class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Room room;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Embedded
    private TimeSlot timeSlot;

    private LocalDate bookingDate;

    Booking(String roomLocation, TimeSlot timeSlot) {
        this.room = new Room(roomLocation);
        this.timeSlot = timeSlot;
        this.status = BookingStatus.CONFIRMED;
        this.bookingDate = LocalDate.now();
    }

    public boolean isConfirmed() {
        return this.status == BookingStatus.CONFIRMED;
    }

    public boolean isCheckedIn() {
        return this.status == BookingStatus.CHECKED_IN;
    }

    public boolean isCanceled() {
        return this.status == BookingStatus.CANCELED;
    }

    public boolean isNoShow() {
        return this.status == BookingStatus.NO_SHOW;
    }

    public Booking checkIn() {
        if (!isConfirmed()) {
            throw new IllegalStateException("Only confirmed bookings can be checked in");
        }
        if (isCheckedIn()) {
            throw new IllegalStateException("Booking is already checked in");
        }

        this.status = BookingStatus.CHECKED_IN;
        return this;
    }

    public Booking cancel() {
        if (isCheckedIn()) {
            throw new IllegalStateException("Checked in bookings cannot be canceled");
        }
        if (isCanceled()) {
            throw new IllegalStateException("Booking is already canceled");
        }

        this.status = BookingStatus.CANCELED;
        return this;
    }

    public Booking markAsNoShow() {
        if (isCanceled()) {
            throw new IllegalStateException("Booking is already canceled");
        }

        this.status = BookingStatus.NO_SHOW;
        return this;
    }



    @Embeddable
    record TimeSlot(

            @Column(name = "start_time", nullable = false)
            LocalDateTime startTime,

            @Column(name = "end_time", nullable = false)
            LocalDateTime endTime
    ) {
        public TimeSlot {
            if (startTime == null) throw new IllegalArgumentException("Start time is required");
            if (endTime == null) throw new IllegalArgumentException("End time is required");
            if (endTime.isBefore(startTime)) throw new IllegalArgumentException("End time must be after start time");
        }
    }

    /**Room modeled as a value object in the Booking Module. It only contains the one property 'name'
     *
     * @param roomLocation The id of the room being booked
     */
    @Embeddable
    record Room(String roomLocation) {
        public Room {
            if (roomLocation == null || roomLocation.isBlank()) throw new IllegalArgumentException("Room location is required");
        }
    }


    public enum BookingStatus {
        CONFIRMED, CHECKED_IN, CANCELED, NO_SHOW
    }
}
