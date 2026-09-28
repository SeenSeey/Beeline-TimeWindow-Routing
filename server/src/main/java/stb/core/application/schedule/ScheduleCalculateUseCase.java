package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.model.*;
import stb.core.domain.port.route.RouteStorage;
import stb.core.domain.port.route.dto.CrewRouteDto;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.port.route.dto.RouteDto;
import stb.core.domain.repository.*;
import stb.core.domain.service.CrewEquipmentDomainService;
import stb.core.domain.service.CrewRouteDomainService;
import stb.core.domain.service.RouteDomainService;
import stb.core.domain.service.SpeedProfileDomainService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleCalculateUseCase {

    private final RequestRepository requestRepository;
    private final ScheduleRepository scheduleRepository;
    private final AlterationReasonRepository reasonRepository;
    private final RequestActionAuditRepository requestActionAuditRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final ScheduleAssemblerDomainService assemblerDomainService;
    private final RouteDomainService routeDomainService;
    private final SpeedProfileDomainService speedProfileDomainService;
    private final CrewEquipmentDomainService crewEquipmentDomainService;
    private final CrewRouteDomainService crewRouteDomainService;
    private final RouteStorage routeStorage;

    @Transactional
    public void execute(LocalDate targetDate) {
        List<Schedule> schedules = scheduleRepository.findAllByTargetDateAndStatus(targetDate, Schedule.Status.CALCULATING);
        List<RequestStatus> statuses = requestStatusRepository.findAll();
        List<RequestActionAudit> audits = new ArrayList<>();
        List<AlterationReason> reasons = new ArrayList<>();

        for (Schedule schedule : schedules) {
            Region region = schedule.getRegion();
            log.info("Расчёт для региона: {}", region.getDisplayName());

            List<Request> requests = requestRepository.findAllForSchedulingByRegionId(region.getId());
            List<EngineerCrew> engineerCrews = engineerCrewRepository.findAllActiveByRegionIdAndTargetDate(region.getId(), targetDate);

            if (requests.isEmpty()) {
                crewEquipmentDomainService.issueInitialLoadout(engineerCrews, List.of());
                schedule.markAsCreated();
                continue;
            }

            List<Location> points = Stream.concat(
                    Stream.of(Location.from(region)),
                    requests.stream().map(Location::from)
            ).toList();

            List<RouteDto> routes = routeDomainService.calculateAllRoutes(points, Vehicle.Keyword.CAR);
            routeStorage.saveRoutes(region, routes);

            ScheduleContext context = new ScheduleContext();
            context.setRequests(requests);
            context.setEngineerCrews(engineerCrews);
            context.setTravelMatrix(new TravelMatrix(routes));
            context.setSpeedProfile(speedProfileDomainService.createSpeedProfile());

            ScheduleCalculationResult result = assemblerDomainService.createSchedule(context);

            Map<Integer, Request> requestById = requests.stream().collect(Collectors.toMap(Request::getId, Function.identity()));
            Map<Integer, EngineerCrew> crewById = engineerCrews.stream().collect(Collectors.toMap(EngineerCrew::getId, Function.identity()));

            List<Request> assignedRequests = new ArrayList<>();

            for (OutputSlotDto slotDto : result.requestDtoList()) {
                Request request = requestById.get(slotDto.requestId());

                if (slotDto.engineerId() == null || slotDto.serviceStartTime() == null || slotDto.departureTime() == null) {
                    log.warn("Не удалось распределить заявку {} в текущее расписание региона {}", slotDto.requestId(), region.getDisplayName());
                    reasons.add(AlterationReason.notPlaced(schedule, request, engineerCrews));
                    continue;
                }

                EngineerCrew crew = crewById.get(slotDto.engineerId());

                request.assignCrew(crew, statuses);
                assignedRequests.add(request);
                schedule.addSlot(
                        request,
                        crew,
                        ScheduleSlot.SlotType.REQUEST,
                        slotDto.arrivalTime(),
                        slotDto.serviceStartTime(),
                        slotDto.departureTime()
                );

                audits.add(RequestActionAudit.assigned(request, crew, slotDto.serviceStartTime(), slotDto.departureTime()));
            }

            Map<Integer, List<ScheduleSlot>> crewLocations = schedule.getSlots()
                    .stream()
                    .collect(Collectors.groupingBy(slot -> slot.getEngineerCrew().getId()));

            for (var entry : crewLocations.entrySet()) {
                EngineerCrew crew = crewById.get(entry.getKey());

                log.info("Расчёт маршрута бригады {} в рамках расписания {}", crew.getName(), schedule.getId());

                // in visit order: the slots come in the solver's request order, not the crew's
                List<Request> visits = entry.getValue()
                        .stream()
                        .sorted(Comparator.comparing(ScheduleSlot::getStartTimeWindow))
                        .map(ScheduleSlot::getRequest)
                        .toList();

                // leg by leg, from the region's office (it is among the points the routes were built for)
                CrewRouteDto crewRoute = crewRouteDomainService.assemble(Location.from(region), visits, routes);

                log.info("Маршрут рассчитан");
                routeStorage.saveRoute(schedule, crew, crewRoute);
            }

            crewEquipmentDomainService.issueInitialLoadout(engineerCrews, assignedRequests);
            schedule.markAsCreated();
        }

        scheduleRepository.saveAll(schedules);
        reasonRepository.saveAll(reasons);
        requestActionAuditRepository.saveAll(audits);
    }
}
