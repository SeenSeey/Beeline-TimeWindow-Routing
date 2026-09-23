package stb.core.domain.service.scheduler.dto;

import lombok.ToString;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
public class TravelMatrix {

    private final Map<String, Double> distances;

    public TravelMatrix() {
        this.distances = new HashMap<>();
    }

    public TravelMatrix(List<RouteDto> routes) {
        this();

        for (RouteDto route : routes) {
            if (route.pointA().id() == null || route.pointB().id() == null) {
                throw new IllegalArgumentException("Необходимо указать ID точки");
            }

            addDistance(route.pointA(), route.pointB(), route.distanceKilometers());
        }
    }

    public void addDistance(Location pointA, Location pointB, double distance) {
        distances.put(key(pointA.id(), pointB.id()), distance);
        distances.put(key(pointB.id(), pointA.id()), distance);
    }

    public double distanceBetween(Location from, Location to) {
        if (from.equals(to)) {
            return 0.0;
        }

        Double distance = distances.get(key(from.id(), to.id()));

        if (distance == null) {
            throw new IllegalArgumentException("Расстояние между точками %s и %s не найдено".formatted(from.id(), to.id()));
        }

        return distance;
    }

    public static String key(String fromId, String toId) {
        return fromId + " -> " + toId;
    }
}