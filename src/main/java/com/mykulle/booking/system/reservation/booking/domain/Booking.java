package com.mykulle.booking.system.reservation.booking.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;

@AggregateRoot
@Entity
@Getter
@NoArgsConstructor
@Table(name = "bookings")
public class Booking {

    @Identity
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Embedded
    private TimeRange timeRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Version
    private Long version;

    public Booking(Long roomId, TimeRange timeRange) {
        if (roomId == null) throw new IllegalArgumentException("roomId is required");
        if (timeRange == null) throw new IllegalArgumentException("timeRange is required");
        this.roomId = roomId;
        this.timeRange = timeRange;
        this.status = BookingStatus.CONFIRMED;
    }

    public static EnumSet<BookingStatus> blockingStatuses() {
        return EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CHECK_IN_REQUIRED, BookingStatus.CHECKED_IN);
    }

    public void requireCheckIn() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Check-in can only be required from CONFIRMED");
        }
        this.status = BookingStatus.CHECK_IN_REQUIRED;
    }

    public void checkIn() {
        if (status != BookingStatus.CHECK_IN_REQUIRED) {
            throw new IllegalStateException("Cannot check in unless status is CHECK_IN_REQUIRED");
        }
        this.status = BookingStatus.CHECKED_IN;
    }

    public void markNoShow() {
        if (status != BookingStatus.CHECK_IN_REQUIRED) {
            throw new IllegalStateException("No-show can only happen from CHECK_IN_REQUIRED");
        }
        this.status = BookingStatus.NO_SHOW;
    }

    public void complete() {
        if (status != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Complete can only happen from CHECKED_IN");
        }
        this.status = BookingStatus.COMPLETED;
    }

    public void cancel(LocalDateTime now) {
        if (now == null) throw new IllegalArgumentException("now is required");

        if (status == BookingStatus.CANCELLED
                || status == BookingStatus.COMPLETED
                || status == BookingStatus.NO_SHOW) {
            throw new IllegalStateException("Cannot cancel booking in status: " + status);
        }

        if (now.isAfter(timeRange.endTime())) {
            throw new IllegalStateException("Cannot cancel after end time");
        }

        this.status = BookingStatus.CANCELLED;
    }

    @ValueObject
    @Embeddable
    public record TimeRange(
            @Column(name = "start_time", nullable = false) LocalDateTime startTime,
            @Column(name = "end_time", nullable = false) LocalDateTime endTime
    ) {
        private static final long MIN_DURATION_MINUTES = 30L;
        private static final long MAX_DURATION_MINUTES = 120L;

        public TimeRange {
            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException("startTime and endTime are required");
            }
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("endTime must be after startTime");
            }

            var durationInMinutes = Duration.between(startTime, endTime).toMinutes();
            if (durationInMinutes < MIN_DURATION_MINUTES) {
                throw new IllegalArgumentException("Booking duration must be at least 30 minutes");
            }
            if (durationInMinutes > MAX_DURATION_MINUTES) {
                throw new IllegalArgumentException("Booking duration must be at most 120 minutes");
            }

            validateAlignedToTimeslot("startTime", startTime);
            validateAlignedToTimeslot("endTime", endTime);
        }

        private static void validateAlignedToTimeslot(String field, LocalDateTime value) {
            if (value.getSecond() != 0 || value.getNano() != 0) {
                throw new IllegalArgumentException(field + " must have zero seconds and nanoseconds");
            }

            var minute = value.getMinute();
            if (minute != 0 && minute != 30) {
                throw new IllegalArgumentException(field + " must align to 30-minute slots");
            }
        }

        public boolean overlaps(TimeRange other) {
            return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
        }
    }

    public enum BookingStatus {
        CONFIRMED,
        CHECK_IN_REQUIRED,
        CHECKED_IN,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }
}
