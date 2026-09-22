package stb.core.infrastructure.route;

import lombok.experimental.UtilityClass;
import stb.core.domain.model.Vehicle;

import java.util.stream.Stream;

@UtilityClass
public class GraphHopperUtils {

    public static final String VEHICLE_CAR = "car";
    public static final String VEHICLE_BICYCLE = "bike";
    public static final String VEHICLE_FOOT = "foot";

    public static Stream<String> getVehicleTypes() {
        return Stream.of(VEHICLE_CAR, VEHICLE_BICYCLE, VEHICLE_FOOT);
    }

    public static String getGraphHopperVehicleName(Vehicle.Keyword vehicleKeyword) {
        return switch (vehicleKeyword) {
            case CAR, PUBLIC_TRANSPORT -> VEHICLE_CAR;
            case BICYCLE -> VEHICLE_BICYCLE;
            case PEDESTRIAN -> VEHICLE_FOOT;
        };
    }
}
