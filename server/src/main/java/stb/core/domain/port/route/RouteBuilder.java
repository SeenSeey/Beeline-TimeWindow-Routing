package stb.core.domain.port.route;

import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.List;

public interface RouteBuilder {

    RouteDto buildRoute(Location pointA, Location pointB, Vehicle.Keyword vehicleType);
    RouteDto mergeRoutes(List<Location> locations, List<RouteDto> routes);
}
