package stb.core.infrastructure.route;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stb.core.domain.port.route.dto.GeoPoint;
import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.RouteBuilder;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class RouteBuilderImpl implements RouteBuilder {

    private final GraphHopper graphHopper;

    @Override
    public RouteDto buildRoute(GeoPoint pointA, GeoPoint pointB, Vehicle.Keyword vehicleType) {
        String vehicle = GraphHopperUtils.getGraphHopperVehicleName(vehicleType);

        GHRequest request = new GHRequest(pointA.latitude(), pointA.longitude(), pointB.latitude(), pointB.longitude())
                .setProfile(vehicle)
                .setLocale(Locale.ROOT);

        GHResponse response = graphHopper.route(request);

        List<GeoPoint> points = new ArrayList<>();
        double distanceKilometers = 10000;

        if (response.hasErrors()) {
            log.error("Ошибка построения маршрута: {}", response.getErrors().stream().map(Throwable::getMessage).toList());
        } else {
            ResponsePath path = response.getBest();

            distanceKilometers = path.getDistance() / 1000f;
            PointList pointList = path.getPoints();

            for (int i = 0; i < pointList.size(); i++) {
                points.add(new GeoPoint(pointList.getLon(i), pointList.getLat(i)));
            }
        }

        return new RouteDto(pointA, pointB, distanceKilometers, points);
    }
}
