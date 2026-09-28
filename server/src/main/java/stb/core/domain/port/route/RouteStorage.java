package stb.core.domain.port.route;

import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Schedule;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.port.route.dto.RouteDto;

import java.util.List;

public interface RouteStorage {

    List<RouteDto> findAllRoutesByRegion(Region region);
    CrewRouteDto findRoute(Schedule schedule, EngineerCrew engineerCrew);
    void saveRoute(Schedule schedule, EngineerCrew engineerCrew, CrewRouteDto route);
    /** Удалить маршрут бригады в версии расписания — у бригады в ней не осталось заявок */
    void deleteRoute(Schedule schedule, EngineerCrew engineerCrew);
    void saveRoutes(Region region, List<RouteDto> routes);

    void clearAll();
    void clearCrewsRoutesBySchedule(Schedule schedule);
}
