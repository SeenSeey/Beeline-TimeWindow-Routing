package stb.core.application.schedule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.AlterationReasonDto;
import stb.core.domain.dto.ScheduleTimelineDto;
import stb.core.domain.dto.UnassignedRequestDto;
import stb.core.domain.exception.RegionNotFoundException;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.model.Schedule;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.repository.AlterationReasonRepository;
import stb.core.domain.repository.EngineerCrewOffShiftRecordRepository;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.ScheduleRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleTimelineFindCurrentByRegionUseCase {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ScheduleRepository scheduleRepository;
    private final RegionRepository regionRepository;
    private final RequestRepository requestRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final EngineerCrewOffShiftRecordRepository engineerCrewOffShiftRecordRepository;
    private final AlterationReasonRepository reasonRepository;
    private final RouteStorage routeStorage;

    @Transactional(readOnly = true)
    public ScheduleTimelineDto execute(int regionId) {
        LocalDate targetDate = LocalDate.now();

        Region region = regionRepository
            .findById(regionId)
            .orElseThrow(() -> new RegionNotFoundException("Регион не существует"));

        Schedule schedule = scheduleRepository
            .findByTargetDateAndRegionId(targetDate, regionId)
            .orElseThrow(() ->
                new ScheduleNotFoundException(
                    "Расписание на %s для региона %s ещё не сформировано".formatted(
                        dateTimeFormatter.format(targetDate),
                        region.getDisplayName()
                    )
                )
            );

        Map<Integer, AlterationReasonDto> reasonsByRequestId = findAlterationReasons(schedule);
        List<EngineerCrew> crews = engineerCrewRepository.findAllByRegionId(regionId);

        return ScheduleTimelineDto.from(
            schedule,
            findUnassignedRequests(schedule, regionId, reasonsByRequestId),
            crews,
            findOffShiftCrewIds(regionId, targetDate),
            reasonsByRequestId,
            findRouteDistances(schedule, crews)
        );
    }

    private Map<Integer, Double> findRouteDistances(Schedule schedule, List<EngineerCrew> crews) {
        if (schedule.getStatus() != Schedule.Status.CREATED) {
            return Map.of();
        }

        Map<Integer, Double> distancesByCrewId = new HashMap<>();
        for (EngineerCrew crew : crews) {
            CrewRouteDto route = routeStorage.findRoute(schedule.getRegion(), crew);

            if (route != null) {
                distancesByCrewId.put(crew.getId(), route.distanceKilometers());
            }
        }
        return distancesByCrewId;
    }

    private Set<Integer> findOffShiftCrewIds(int regionId, LocalDate targetDate) {
        return engineerCrewOffShiftRecordRepository
            .findAllByEngineerCrewRegionIdAndOffShiftDate(regionId, targetDate)
            .stream()
            .map(record -> record.getEngineerCrew().getId())
            .collect(Collectors.toSet());
    }

    private Map<Integer, AlterationReasonDto> findAlterationReasons(Schedule schedule) {
        return reasonRepository.findAllDistinctByScheduleIdOrderById(schedule.getId()).stream()
                .map(AlterationReasonDto::from)
                .collect(Collectors.toMap(AlterationReasonDto::requestId, reason -> reason));
    }

    private List<UnassignedRequestDto> findUnassignedRequests(Schedule schedule,
                                                              int regionId,
                                                              Map<Integer, AlterationReasonDto> reasonsByRequestId) {
        if (schedule.getStatus() != Schedule.Status.CREATED) {
            return List.of();
        }

        return requestRepository
            .findAllUnassignedByRegionIdAndStatus(regionId, RequestStatus.Keyword.IDLE)
            .stream()
            .map(request -> UnassignedRequestDto.from(request, reasonsByRequestId.get(request.getId())))
            .toList();
    }
}
