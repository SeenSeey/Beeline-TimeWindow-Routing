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
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RouteBuilderImpl implements RouteBuilder {

    private final GraphHopper graphHopper;

    @Override
    public RouteDto buildRoute(Location pointA, Location pointB, Vehicle.Keyword vehicleType) {
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

            distanceKilometers = path.getDistance() / 1000.0;
            PointList pointList = path.getPoints();

            for (int i = 0; i < pointList.size(); i++) {
                points.add(new GeoPoint(pointList.getLat(i), pointList.getLon(i)));
            }
        }

        return new RouteDto(pointA, pointB, distanceKilometers, points);
    }

    @Override
    public RouteDto mergeRoutes(List<Location> locations, List<RouteDto> routes) {
        if (locations.size() < 2) {
            throw new IllegalArgumentException("Невозможно рассчитать маршрут между точками: %s".formatted(locations));
        }

        Map<String, RouteDto> routesBetweenGeoPoints = routes
                .stream()
                .collect(Collectors.toMap(
                        route -> buildKey(route.pointA(), route.pointB()),
                        Function.identity(),
                        (left, _) -> left
                ));

        double distanceKilometers = 0;
        List<GeoPoint> points = new ArrayList<>();

        for (int i = 0; i < locations.size() - 1; i++) {
            Location currentLocation = locations.get(i);
            Location nextLocation = locations.get(i + 1);

            // two requests at one address: no leg to travel
            if (currentLocation.id().equals(nextLocation.id())) {
                continue;
            }

            RouteDto leg = legBetween(routesBetweenGeoPoints, currentLocation, nextLocation);
            List<GeoPoint> legPoints = leg.points();

            distanceKilometers += leg.distanceKilometers();
            // a leg starts where the previous one ended: don't repeat the joint
            points.addAll(points.isEmpty() || legPoints.isEmpty() ? legPoints : legPoints.subList(1, legPoints.size()));
        }

        return new RouteDto(locations.getFirst(), locations.getLast(), distanceKilometers, points);
    }

    /**
     * The leg from {@code from} to {@code to}, its points in travel order. Routes are calculated once per
     * pair of points (see RouteDomainService.calculateAllRoutes), so a leg travelled the other way is the
     * stored route reversed.
     */
    private RouteDto legBetween(Map<String, RouteDto> routesBetweenGeoPoints, Location from, Location to) {
        RouteDto forward = routesBetweenGeoPoints.get(buildKey(from, to));
        if (forward != null) {
            return forward;
        }

        RouteDto backward = routesBetweenGeoPoints.get(buildKey(to, from));
        if (backward == null) {
            throw new RuntimeException("Не удалось найти расстояние между точками: %s и %s".formatted(from, to));
        }

        List<GeoPoint> reversedPoints = new ArrayList<>(backward.points());
        Collections.reverse(reversedPoints);
        return new RouteDto(from, to, backward.distanceKilometers(), reversedPoints);
    }

    private String buildKey(Location pointA, Location pointB) {
        return pointA.id() + "_" + pointB.id();
    }
}
