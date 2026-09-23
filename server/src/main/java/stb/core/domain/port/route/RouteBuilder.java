package stb.core.domain.port.route;

import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;

public interface RouteBuilder {

    RouteDto buildRoute(Location pointA, Location pointB, Vehicle.Keyword vehicleType);
}
