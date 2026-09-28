package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.AlterationReasonDto;
import stb.core.domain.dto.ScheduleTimelineDto;
import stb.core.domain.dto.UnassignedRequestDto;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.repository.AlterationReasonRepository;
import stb.core.domain.repository.EngineerCrewOffShiftRecordRepository;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.ScheduleRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Any version of a schedule by its id — for looking at an earlier one, the schedule before a recalculation.
 * Shown as it was calculated: its slots, the reasons it moved requests, the requests it left out, the
 * region's crews and their route lengths in that version.
 */
@Component
@RequiredArgsConstructor
public class ScheduleTimelineFindByIdUseCase {

    private final ScheduleRepository scheduleRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final EngineerCrewOffShiftRecordRepository engineerCrewOffShiftRecordRepository;
    private final AlterationReasonRepository reasonRepository;
    private final RouteStorage routeStorage;

    @Transactional(readOnly = true)
    public ScheduleTimelineDto execute(int scheduleId) {
        Schedule schedule = scheduleRepository
                .findWithSlotsById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("Расписание с ID = %s не найдено".formatted(scheduleId)));

        int regionId = schedule.getRegion().getId();
        List<EngineerCrew> crews = engineerCrewRepository.findAllByRegionId(regionId);
        List<AlterationReason> reasons = reasonRepository.findAllDistinctByScheduleIdOrderById(schedule.getId());
        Map<Integer, AlterationReasonDto> reasonsByRequestId = toReasonsByRequestId(reasons);

        return ScheduleTimelineDto.from(
                schedule,
                findLeftOutRequests(schedule, reasons, reasonsByRequestId),
                crews,
                findOffShiftCrewIds(regionId, schedule),
                reasonsByRequestId,
                findRouteDistances(schedule, crews)
        );
    }

    /**
     * The requests this version left out, as it left them: the ones it has a reason for (a calculation records
     * one for every request it can't place, a recalculation for every request it drops) but no slot. Unlike
     * the current schedule's list, this isn't the requests' state now — which says nothing about an old
     * version. Urgent first, then by deadline, like the current list
     */
    private List<UnassignedRequestDto> findLeftOutRequests(Schedule schedule,
                                                           List<AlterationReason> reasons,
                                                           Map<Integer, AlterationReasonDto> reasonsByRequestId) {
        Schedule.Status status = schedule.getStatus();
        if (status != Schedule.Status.CREATED && status != Schedule.Status.OUTDATED) {
            return List.of();
        }

        Set<Integer> scheduledRequestIds = schedule.getSlots()
                .stream()
                .map(slot -> slot.getRequest().getId())
                .collect(Collectors.toSet());

        Map<Integer, Request> leftOut = new LinkedHashMap<>();
        reasons.stream()
                .map(AlterationReason::getRequest)
                .filter(request -> !scheduledRequestIds.contains(request.getId()))
                .forEach(request -> leftOut.putIfAbsent(request.getId(), request));

        return leftOut.values()
                .stream()
                .sorted(Comparator.comparing(Request::isUrgent).reversed().thenComparing(Request::getDeadline))
                .map(request -> UnassignedRequestDto.from(request, reasonsByRequestId.get(request.getId())))
                .toList();
    }

    /**
     * crew id → the length of its route in this version; every version keeps its own routes. Not for a
     * version still being calculated, whose routes aren't final yet
     */
    private Map<Integer, Double> findRouteDistances(Schedule schedule, List<EngineerCrew> crews) {
        Schedule.Status status = schedule.getStatus();
        if (status != Schedule.Status.CREATED && status != Schedule.Status.OUTDATED) {
            return Map.of();
        }

        Map<Integer, Double> distancesByCrewId = new HashMap<>();
        for (EngineerCrew crew : crews) {
            CrewRouteDto route = routeStorage.findRoute(schedule, crew);
            if (route != null) {
                distancesByCrewId.put(crew.getId(), route.distanceKilometers());
            }
        }
        return distancesByCrewId;
    }

    private Set<Integer> findOffShiftCrewIds(int regionId, Schedule schedule) {
        return engineerCrewOffShiftRecordRepository
                .findAllByEngineerCrewRegionIdAndOffShiftDate(regionId, schedule.getTargetDate())
                .stream()
                .map(record -> record.getEngineerCrew().getId())
                .collect(Collectors.toSet());
    }

    private static Map<Integer, AlterationReasonDto> toReasonsByRequestId(List<AlterationReason> reasons) {
        return reasons.stream()
                .map(AlterationReasonDto::from)
                // a request can have several reasons in one schedule: keep the latest (they come ordered by id)
                .collect(Collectors.toMap(AlterationReasonDto::requestId, reason -> reason, (earlier, later) -> later));
    }
}
