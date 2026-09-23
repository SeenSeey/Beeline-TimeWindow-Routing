package stb.core.domain.port.route.dto;

import java.util.List;

public record RouteDto(Location pointA, Location pointB, double distanceKilometers, List<GeoPoint> points) {
}
