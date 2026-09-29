package stb.core.domain.repository.projection;

import java.time.LocalTime;

public interface SpeedIntervalProjection {

    LocalTime getStartTime();
    LocalTime getEndTime();
    double getCoefficient();
}
