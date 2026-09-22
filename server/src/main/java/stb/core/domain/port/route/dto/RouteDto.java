package stb.core.domain.port.route.dto;

import java.util.List;

public record RouteDto(GeoPoint pointA, GeoPoint pointB, double distanceKilometers, List<GeoPoint> points) {
}
