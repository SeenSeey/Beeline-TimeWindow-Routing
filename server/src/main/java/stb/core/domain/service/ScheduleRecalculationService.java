package stb.core.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stb.core.domain.dto.ScheduleHistoryResult;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;
import stb.core.domain.repository.*;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.repository.projection.EngineerCrewSlotsCount;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.ScheduleContext;
import stb.core.domain.service.scheduler.dto.TravelMatrix;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final CrewRouteDomainService crewRouteDomainService;
    private final RouteStorage routeStorage;

    public ScheduleHistoryResult recalculateSchedule(Region region, LocalDate targetDate, List<RouteDto> regionRoutes) {
        Schedule currentSchedule = scheduleRepository.findByTargetDateAndRegionId(targetDate, region.getId())
                .orElseThrow(() -> new ScheduleNotFoundException("Расписание на %s для региона %s не найдено".formatted(targetDate, region.getId())));

        List<RouteDto> routes = new ArrayList<>(regionRoutes);

        List<Request> requestsToReschedule = requestRepository.findAllForRescheduleByRegionId(region.getId());
        List<EngineerCrew> engineerCrews = engineerCrewRepository.findAllActiveByRegionIdAndTargetDate(region.getId(), targetDate);
        List<CrewLatestSlot> crewLatestSlots = scheduleSlotRepository.findLatestSlotsForCrewsByRegionId(region.getId());
        List<RequestStatus> statuses = requestStatusRepository.findAll();

        Map<Integer, Request> requestById = requestsToReschedule.stream().collect(Collectors.toMap(Request::getId, Function.identity()));
        Map<Integer, EngineerCrew> crewById = engineerCrews.stream().collect(Collectors.toMap(EngineerCrew::getId, Function.identity()));
        Map<Integer, Integer> slotsCountByCrewId = scheduleSlotRepository.countAllByScheduleAndEngineerCrewIdIn(
                currentSchedule.getId(),
                engineerCrews.stream().map(EngineerCrew::getId).toList()
        ).stream().collect(Collectors.toMap(EngineerCrewSlotsCount::getEngineerCrewId, EngineerCrewSlotsCount::getActiveSlotsCount));

        List<ScheduleSlot> staticSlots = currentSchedule.getSlots()
                .stream()
                .filter(slot -> slot.getRequest().isInProgress() || slot.getRequest().isCompleted())
                .toList();

        Map<Integer, ScheduleSlot> slotByRequestId = currentSchedule.getSlots()
                .stream()
                .collect(Collectors.toMap(slot -> slot.getRequest().getId(), Function.identity()));

        if (routes.isEmpty()) {
            List<Location> points = Stream.concat(Stream.concat(
                            Stream.of(Location.from(region)),
                            requestsToReschedule.stream().map(Location::from)),
                    crewLatestSlots.stream().map(Location::from)
            ).toList();

            routes.addAll(routeDomainService.calculateAllRoutes(points, Vehicle.Keyword.CAR));
            routeStorage.saveRoutes(region, routes);
        }

        ScheduleContext context = ScheduleContext.builder()
                .requests(requestsToReschedule)
                .engineerCrews(engineerCrews)
                .travelMatrix(new TravelMatrix(routes))
                .speedProfile(speedProfileDomainService.createSpeedProfile())
                .crewsLatestSlots(crewLatestSlots)
                .crewsUsedInSchedule(slotsCountByCrewId)
                .replanning(true)
                .build();

        ScheduleCalculationResult result = scheduleAssemblerDomainService.createSchedule(context);

        Schedule schedule = Schedule.createFrom(currentSchedule);
        staticSlots.forEach(schedule::addSlot);

        List<Request> updatedRequests = new ArrayList<>();

        List<ScheduleSlot> assigned = new ArrayList<>();
        List<ScheduleSlot> reassigned = new ArrayList<>();
        List<Request> unassigned = new ArrayList<>();
        List<Request> notPlaced = new ArrayList<>();
        List<ScheduleHistoryResult.StartTimeChange> startTimeChanged = new ArrayList<>();

        for (OutputSlotDto slotDto : result.requestDtoList()) {
            Request request = requestById.get(slotDto.requestId());
            ScheduleSlot previousSlot = slotByRequestId.get(request.getId());

            if (slotDto.engineerId() == null || slotDto.serviceStartTime() == null || slotDto.departureTime() == null) {
                log.warn("Не удалось распределить заявку {} в текущее расписание региона {}", slotDto.requestId(), region.getDisplayName());
                request.unassign(statuses);

                if (previousSlot != null) {
                    unassigned.add(request);
                } else {
                    notPlaced.add(request);
                }

                updatedRequests.add(request);

                continue;
            }

            request.assignCrew(crewById.get(slotDto.engineerId()), statuses);

            ScheduleSlot slot = schedule.addSlot(
                    request,
                    crewById.get(slotDto.engineerId()),
                    ScheduleSlot.SlotType.REQUEST,
                    slotDto.arrivalTime(),
                    slotDto.serviceStartTime(),
                    slotDto.departureTime()
            );

            if (previousSlot != null) {
                if (slotCrewChanged(previousSlot, slot)) {
                    reassigned.add(slot);
                } else if (slotStartTimeChanged(previousSlot, slot)) {
                    startTimeChanged.add(new ScheduleHistoryResult.StartTimeChange(slot, previousSlot.getStartTimeWindow()));
                }
            } else {
                assigned.add(slot);
            }

            updatedRequests.add(request);
        }

        routeStorage.clearCrewsRoutesByRegion(region);

        schedule.getSlots()
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getEngineerCrew().getId()))
                .forEach((crewId, slots) -> crewRouteDomainService.recalculateCrewRoute(region, crewById.get(crewId), slots, routes));

        scheduleRepository.save(schedule);
        requestRepository.saveAll(updatedRequests);

        return new ScheduleHistoryResult(
                startTimeChanged,
                assigned,
                reassigned,
                unassigned,
                notPlaced,
                engineerCrews,
                schedule
        );
    }

    private boolean slotStartTimeChanged(ScheduleSlot previousSlot, ScheduleSlot currentSlot) {
        if (previousSlot == null || currentSlot == null) {
            return false;
        }

        return !(previousSlot.getStartTimeWindow().equals(currentSlot.getStartTimeWindow())
                && previousSlot.getEndTimeWindow().equals(currentSlot.getEndTimeWindow()));
    }

    private boolean slotCrewChanged(ScheduleSlot previousSlot, ScheduleSlot currentSlot) {
        if (previousSlot == null || currentSlot == null) {
            return false;
        }

        return previousSlot.getEngineerCrew().getId() != currentSlot.getEngineerCrew().getId();
    }
}
