package stb.core.application.crew;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.CrewDetailedDto;
import stb.core.domain.exception.CrewNotFoundException;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.repository.ScheduleSlotRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CrewFindByIdUseCase {

    private final EngineerCrewRepository engineerCrewRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RequestRepository requestRepository;
    private final ScheduleRepository scheduleRepository;
    private final RouteStorage routeStorage;

    @Transactional(readOnly = true)
    public CrewDetailedDto execute(int crewId) {
        EngineerCrew crew = engineerCrewRepository
                .findById(crewId)
                .orElseThrow(() -> new CrewNotFoundException("Бригада №%d не найдена".formatted(crewId)));

        LocalDate today = LocalDate.now();
        List<ScheduleSlot> routeSlots =
                scheduleSlotRepository.findAllByTargetDateAndCrewIdAndType(today, crewId, ScheduleSlot.SlotType.REQUEST);

        return CrewDetailedDto.from(
                crew,
                today,
                routeSlots,
                findRoadRoute(crew, today),
                requestRepository.findAllByAssignedCrewId(crewId)
        );
    }

    /**
     * Маршрут бригады по дорогам в текущем расписании её региона — том же, что показывает доска выездов;
     * только для рассчитанного расписания
     */
    private CrewRouteDto findRoadRoute(EngineerCrew crew, LocalDate targetDate) {
        return scheduleRepository
                .findByTargetDateAndRegionId(targetDate, crew.getRegion().getId())
                .filter(schedule -> schedule.getStatus() == Schedule.Status.CREATED)
                .map(schedule -> routeStorage.findRoute(schedule.getRegion(), crew))
                .orElse(null);
    }
}
