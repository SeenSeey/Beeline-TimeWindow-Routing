package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stb.core.domain.common.PolylineEncoder;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.port.route.dto.GeoPoint;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrewRouteDomainService {

    private final RouteStorage routeStorage;

    public void recalculateCrewRoute(Region region, EngineerCrew crew, List<ScheduleSlot> slots, List<RouteDto> routes) {
        log.info("Расчёт маршрута бригады {}", crew.getName());

        List<Request> visits = slots
                .stream()
                .sorted(Comparator.comparing(ScheduleSlot::getStartTimeWindow))
                .map(ScheduleSlot::getRequest)
                .toList();

        CrewRouteDto crewRoute = assemble(Location.from(region), visits, routes);
        routeStorage.saveRoute(region, crew, crewRoute);

        log.info("Маршрут рассчитан");
    }

    /**
     * Собрать маршрут бригады по участкам: от {@code start} (офис региона) до первой заявки, затем от
     * заявки к заявке в порядке посещения.
     *
     * @param start  откуда бригада выезжает
     * @param visits заявки в порядке посещения
     * @param routes маршруты между всеми точками, по одному на пару (см. {@link RouteDomainService#calculateAllRoutes})
     */
    public CrewRouteDto assemble(Location start, List<Request> visits, List<RouteDto> routes) {
        Map<String, RouteDto> routesByEnds = routes
                .stream()
                .collect(Collectors.toMap(route -> key(route.pointA(), route.pointB()), Function.identity(), (left, _) -> left));

        List<CrewRouteDto.Leg> legs = new ArrayList<>();
        double distanceKilometers = 0;

        Location from = start;
        Integer fromRequestId = null;

        for (Request visit : visits) {
            Location to = Location.from(visit);

            // two requests at one address: the leg is there, with nothing to travel
            RouteDto leg = from.id().equals(to.id())
                    ? new RouteDto(from, to, 0, List.of())
                    : legBetween(routesByEnds, from, to);

            legs.add(new CrewRouteDto.Leg(
                    legs.size(),
                    fromRequestId,
                    visit.getId(),
                    leg.distanceKilometers(),
                    PolylineEncoder.encode(leg.points())
            ));
            distanceKilometers += leg.distanceKilometers();

            from = to;
            fromRequestId = visit.getId();
        }

        return new CrewRouteDto(distanceKilometers, legs);
    }

    /**
     * Участок от {@code from} до {@code to}, точки в порядке движения. Маршрут строится один раз на пару точек,
     * поэтому участок в обратную сторону — это сохранённый маршрут, развёрнутый задом наперёд.
     */
    private static RouteDto legBetween(Map<String, RouteDto> routesByEnds, Location from, Location to) {
        RouteDto forward = routesByEnds.get(key(from, to));
        if (forward != null) {
            return forward;
        }

        RouteDto backward = routesByEnds.get(key(to, from));
        if (backward == null) {
            throw new IllegalStateException("Не удалось найти маршрут между точками: %s и %s".formatted(from, to));
        }

        List<GeoPoint> reversedPoints = new ArrayList<>(backward.points());
        Collections.reverse(reversedPoints);
        return new RouteDto(from, to, backward.distanceKilometers(), reversedPoints);
    }

    private static String key(Location from, Location to) {
        return from.id() + "_" + to.id();
    }
}
