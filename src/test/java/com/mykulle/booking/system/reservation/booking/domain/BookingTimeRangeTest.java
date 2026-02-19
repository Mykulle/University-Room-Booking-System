package com.mykulle.booking.system.reservation.booking.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTimeRangeTest {

    @Test
    void constructor_acceptsAlignedRangeWithinBounds() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 0);
        var end = start.plusMinutes(90);

        var timeRange = new Booking.TimeRange(start, end);

        assertThat(timeRange.startTime()).isEqualTo(start);
        assertThat(timeRange.endTime()).isEqualTo(end);
    }

    @Test
    void constructor_throws_whenTimesAreNotAlignedToThirtyMinutes() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 15);
        var end = LocalDateTime.of(2026, 2, 17, 10, 45);

        assertThatThrownBy(() -> new Booking.TimeRange(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("align to 30-minute slots");
    }

    @Test
    void constructor_throws_whenSecondsOrNanosecondsAreSet() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 0, 1);
        var end = LocalDateTime.of(2026, 2, 17, 10, 30, 1);

        assertThatThrownBy(() -> new Booking.TimeRange(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero seconds and nanoseconds");
    }

    @Test
    void constructor_throws_whenDurationIsOutsideAllowedBounds() {
        var start = LocalDateTime.of(2026, 2, 17, 10, 0);

        assertThatThrownBy(() -> new Booking.TimeRange(start, start.plusMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 30 minutes");

        assertThatThrownBy(() -> new Booking.TimeRange(start, start.plusMinutes(150)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 120 minutes");
    }
}
