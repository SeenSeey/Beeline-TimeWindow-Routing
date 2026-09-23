package stb.core.domain.service.scheduler.impl.logic.speed;

import java.time.LocalTime;

public record SpeedInterval(
        LocalTime start,
        LocalTime end,
        double coefficient
) {

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