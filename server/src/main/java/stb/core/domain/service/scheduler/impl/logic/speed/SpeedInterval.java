package stb.core.domain.service.scheduler.impl.logic.speed;

import stb.core.domain.repository.projection.SpeedIntervalProjection;

import java.time.LocalTime;

public record SpeedInterval(
        LocalTime start,
        LocalTime end,
        double coefficient
) {

    public static SpeedInterval from(SpeedIntervalProjection projection) {
        LocalTime start = projection.getStartTime();
        LocalTime end = projection.getEndTime().equals(LocalTime.of(23, 59))
                ? LocalTime.MAX
                : projection.getEndTime();

        return new SpeedInterval(start, end, projection.getCoefficient());
    }

    public SpeedInterval {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Speed interval start/end must not be null.");
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Speed interval start must be before end.");
        }

        if (coefficient <= 0) {
            throw new IllegalArgumentException("Speed coefficient must be greater than 0.");
        }
    }

    public boolean contains(LocalTime time) {
        return (!time.isBefore(start)) && time.isBefore(end);
    }
}