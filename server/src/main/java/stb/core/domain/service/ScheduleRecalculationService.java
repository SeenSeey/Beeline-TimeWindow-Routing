package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;
import stb.core.domain.replanning.ScheduleVersionDiff;
import stb.core.domain.replanning.ScheduleVersionDiffDomainService;
import stb.core.domain.repository.*;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.ScheduleContext;
import stb.core.domain.service.scheduler.dto.TravelMatrix;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleRecalculationService {

    private final RequestRepository requestRepository;
    private final ScheduleRepository scheduleRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final ScheduleAssemblerDomainService scheduleAssemblerDomainService;
    private final SpeedProfileDomainService speedProfileDomainService;
    private final RouteDomainService routeDomainService;
    private final ScheduleVersionDiffDomainService scheduleVersionDiffDomainService;
    private final UnscheduledRequestReasonDomainService unscheduledReasonDomainService;

    public Recalculation recalculateSchedule(Region region, LocalDate targetDate, LocalDateTime timestamp) {
        Schedule currentSchedule = scheduleRepository.findByTargetDateAndRegionId(targetDate, region.getId())
                .orElseThrow(() -> new ScheduleNotFoundException("Расписание на %s для региона %s не найдено".formatted(targetDate, region.getId())));

        List<Request> requestsToReschedule = requestRepository.findAllForRescheduleByRegionId(region.getId());
        List<EngineerCrew> engineerCrews = engineerCrewRepository.findAllActiveByRegionIdAndTargetDate(region.getId(), targetDate);
        List<CrewLatestSlot> crewLatestSlots = scheduleSlotRepository.findLatestSlotsForCrewsByRegionId(region.getId());
        List<RequestStatus> statuses = requestStatusRepository.findAll();

        Map<Integer, Request> requestById = requestsToReschedule.stream().collect(Collectors.toMap(Request::getId, Function.identity()));
        Map<Integer, EngineerCrew> crewById = engineerCrews.stream().collect(Collectors.toMap(EngineerCrew::getId, Function.identity()));

        List<ScheduleSlot> staticSlots = currentSchedule.getSlots()
                .stream()
                .filter(slot -> slot.getRequest().isInProgress() || slot.getRequest().isCompleted())
                .toList();

        List<Location> points = Stream.concat(Stream.concat(
                        Stream.of(Location.from(region)),
                        requestsToReschedule.stream().map(Location::from)),
                crewLatestSlots.stream().map(Location::from)
        ).toList();

        List<RouteDto> routes = routeDomainService.calculateAllRoutes(points, Vehicle.Keyword.CAR);

        ScheduleContext context = ScheduleContext.builder()
                .requests(requestsToReschedule)
                .engineerCrews(engineerCrews)
                .travelMatrix(new TravelMatrix(routes))
                .speedProfile(speedProfileDomainService.createSpeedProfile())
                .crewsLatestSlots(crewLatestSlots)
                .replanning(true)
                .build();

        ScheduleCalculationResult result = scheduleAssemblerDomainService.createSchedule(context);

        Schedule schedule = Schedule.createFrom(currentSchedule);
        staticSlots.forEach(schedule::addSlot);

        // TODO: отмечать историю
        for (OutputSlotDto slotDto : result.requestDtoList()) {
            if (slotDto.engineerId() == null || slotDto.serviceStartTime() == null || slotDto.departureTime() == null) {
                log.warn("Не удалось распределить заявку {} в текущее расписание региона {}", slotDto.requestId(), region.getDisplayName());
                continue;
            }

            schedule.addSlot(
                    requestById.get(slotDto.requestId()),
                    crewById.get(slotDto.engineerId()),
                    ScheduleSlot.SlotType.REQUEST,
                    slotDto.arrivalTime(),
                    slotDto.serviceStartTime(),
                    slotDto.departureTime()
            );
        }

        ScheduleVersionDiff diff = scheduleVersionDiffDomainService.compare(currentSchedule, schedule);
        scheduleRepository.save(schedule);
        requestRepository.saveAll(applyAssignments(diff, statuses));
        return new Recalculation(schedule, diff, unscheduledReasonDomainService.analyze(
                schedule, requestsToReschedule, engineerCrews, true, timestamp));
    }

    private static List<Request> applyAssignments(ScheduleVersionDiff diff, List<RequestStatus> statuses) {
        Map<Integer, Request> updated = new LinkedHashMap<>();
        diff.removed().forEach(slot -> {
            slot.getRequest().unassign(statuses);
            updated.put(slot.getRequest().getId(), slot.getRequest());
        });
        diff.added().forEach(slot -> {
            slot.getRequest().assignCrew(slot.getEngineerCrew(), statuses);
            updated.put(slot.getRequest().getId(), slot.getRequest());
        });
        diff.moved().forEach(change -> {
            EngineerCrew previousCrew = change.previousSlot().getEngineerCrew();
            EngineerCrew currentCrew = change.currentSlot().getEngineerCrew();
            if (previousCrew.getId() != currentCrew.getId()) {
                change.currentSlot().getRequest().assignCrew(currentCrew, statuses);
                updated.put(change.currentSlot().getRequest().getId(), change.currentSlot().getRequest());
            }
        });
        return List.copyOf(updated.values());
    }

    public record Recalculation(Schedule schedule,
                                ScheduleVersionDiff diff,
                                List<AlterationReason> unscheduledReasons) {
    }
}
