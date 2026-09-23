package stb.core.domain.service.scheduler.impl.logic.speed;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Getter
public class SpeedProfile {

    private final List<SpeedInterval> intervals;

    public SpeedProfile(List<SpeedInterval> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            throw new IllegalArgumentException("Speed profile must contain intervals.");
        }

        this.intervals = List.copyOf(intervals);
        validate();
    }

    public LocalTime calculateArrivalTime(double distanceKm, double transportAverageSpeed, LocalTime departureTime) {
        if (distanceKm < 0) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }

        if (departureTime == null) {
            throw new IllegalArgumentException("Departure time must not be null.");
        }

        if (distanceKm == 0) {
            return departureTime;
        }

        double remainingDistanceKm = distanceKm;
        LocalTime currentTime = departureTime;

        while (remainingDistanceKm > 0) {

            SpeedInterval interval =
                    findInterval(currentTime);

            if (interval == null){
                return null;
            }

            double effectiveSpeedKmh =
                    transportAverageSpeed * interval.coefficient();

            long availableMinutes =
                    Duration.between(
                            currentTime,
                            interval.end()
                    ).toMinutes();
            if (availableMinutes <= 0) {
                currentTime = interval.end();
                continue;
            }

            double distanceAvailableKm =
                    effectiveSpeedKmh
                            * availableMinutes
                            / 60.0;

            if (remainingDistanceKm <= distanceAvailableKm) {

                double requiredMinutes =
                        remainingDistanceKm
                                / effectiveSpeedKmh
                                * 60.0;

                long minutes =
                        (long) Math.ceil(requiredMinutes);


                if (minutes >
                        Duration.between(
                                currentTime,
                                LocalTime.MAX
                        ).toMinutes()) {

                    return null;
                }

                return currentTime.plusMinutes(minutes);
            }

            remainingDistanceKm -= distanceAvailableKm;
            currentTime = interval.end();

        }
        return null;



    }

    private SpeedInterval findInterval(LocalTime time) {
        for (SpeedInterval interval : intervals) {
            if (interval.contains(time)) {
                return interval;
            }
        }
        return null;
    }

    private void validate() {
        for (int i = 0; i < intervals.size(); i++) {
            SpeedInterval current = intervals.get(i);

            if (i == 0 && !current.start().equals(LocalTime.MIDNIGHT)) {
                throw new IllegalArgumentException("Speed profile must start at 00:00.");
            }

            if (i > 0) {
                SpeedInterval previous = intervals.get(i - 1);

                if (!previous.end().equals(current.start())) {
                    throw new IllegalArgumentException("Speed profile intervals must be continuous: %s != %s".formatted(previous.end(), current.start()));
                }
            }
        }

        SpeedInterval last = intervals.getLast();

        if (!last.end().equals(LocalTime.MAX)) {
            throw new IllegalArgumentException("Last speed interval must end at LocalTime.MAX.");
        }
    }
}