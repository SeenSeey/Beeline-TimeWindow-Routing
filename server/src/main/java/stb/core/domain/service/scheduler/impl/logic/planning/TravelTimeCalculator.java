package stb.core.domain.service.scheduler.impl.logic.planning;

import stb.core.domain.port.route.dto.Location;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;
import stb.core.domain.service.scheduler.dto.TravelMatrix;

import java.time.LocalTime;

public final class TravelTimeCalculator {

    private TravelTimeCalculator() {
    }

    public static LocalTime calculateArrival(
            Location from,
            Location to,
            double transportAverageSpeed,
            LocalTime departureTime,
            TravelMatrix travelMatrix,
            SpeedProfile speedProfile
    ) {
        double distanceKm = travelMatrix.distanceBetween(from, to);

        return speedProfile.calculateArrivalTime(
                distanceKm,
                transportAverageSpeed,
                departureTime
        );
    }
}