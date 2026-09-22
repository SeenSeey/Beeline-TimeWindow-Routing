package stb.core.domain.port.route;

import stb.core.domain.port.route.dto.GeoPoint;
import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.dto.RouteDto;

public interface RouteBuilder {

    RouteDto buildRoute(GeoPoint pointA, GeoPoint pointB, Vehicle.Keyword vehicleType);
}
