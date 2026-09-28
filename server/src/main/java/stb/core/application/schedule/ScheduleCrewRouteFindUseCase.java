package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.exception.DomainException;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Schedule;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.ScheduleRepository;

@Component
@RequiredArgsConstructor
public class ScheduleCrewRouteFindUseCase {

    private final ScheduleRepository scheduleRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final RouteStorage routeStorage;

    /** Маршрут бригады в расписании по участкам — для карты выбранной бригады на доске выездов */
    @Transactional(readOnly = true)
    public CrewRouteDto execute(int scheduleId, int crewId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("Расписание с ID = %s не найдено".formatted(scheduleId)));

        EngineerCrew crew = engineerCrewRepository.findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригада с ID = %s не найдена".formatted(crewId)));

        // any version, current or earlier: each keeps its own routes
        CrewRouteDto route = routeStorage.findRoute(schedule, crew);

        if (route == null) {
            throw new DomainException.NotFoundException(
                    "Маршрут бригады %s в расписании %s не найден".formatted(crew.getName(), scheduleId));
        }

        return route;
    }
}
