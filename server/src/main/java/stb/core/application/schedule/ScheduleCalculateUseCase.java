package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.model.*;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.service.RouteDomainService;
import stb.core.domain.service.SpeedProfileDomainService;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.ScheduleContext;
import stb.core.domain.service.scheduler.dto.TravelMatrix;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;

import java.time.LocalDate;
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
    private final EngineerCrewRepository engineerCrewRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final ScheduleAssemblerDomainService assemblerDomainService;
    private final RouteDomainService routeDomainService;
    private final SpeedProfileDomainService speedProfileDomainService;

    @Transactional
    public void execute(LocalDate targetDate) {
        List<Schedule> schedules = scheduleRepository.findAllByTargetDateAndStatus(targetDate, Schedule.Status.CALCULATING);
        List<RequestStatus> statuses = requestStatusRepository.findAll();

        for (Schedule schedule : schedules) {
            Region region = schedule.getRegion();
            log.info("Расчёт для региона: {}", region.getDisplayName());

            List<Request> requests = requestRepository.findAllForSchedulingByRegionId(region.getId());

            if (requests.isEmpty()) {
                schedule.markAsCreated();
                continue;
            }

            List<EngineerCrew> engineerCrews = engineerCrewRepository.findAllActiveByRegionIdAndTargetDate(region.getId(), targetDate);

            List<Location> points = Stream.concat(
                    Stream.of(Location.from(region)),
                    requests.stream().map(Location::from)
            ).toList();

            ScheduleContext context = new ScheduleContext();
            context.setRequests(requests);
            context.setEngineerCrews(engineerCrews);
            context.setTravelMatrix(new TravelMatrix(routeDomainService.calculateAllRoutes(points, Vehicle.Keyword.CAR)));
            context.setSpeedProfile(speedProfileDomainService.createSpeedProfile());

            ScheduleCalculationResult result = assemblerDomainService.createSchedule(context);

            Map<Integer, Request> requestById = requests.stream().collect(Collectors.toMap(Request::getId, Function.identity()));
            Map<Integer, EngineerCrew> crewById = engineerCrews.stream().collect(Collectors.toMap(EngineerCrew::getId, Function.identity()));

            for (OutputSlotDto slotDto : result.requestDtoList()) {
                if (slotDto.engineerId() == null || slotDto.serviceStartTime() == null || slotDto.departureTime() == null) {
                    log.warn("Не удалось распределить заявку {} в текущее расписание региона {}", slotDto.requestId(), region.getDisplayName());
                    continue;
                }

                Request request = requestById.get(slotDto.requestId());

                request.assignCrew(crewById.get(slotDto.engineerId()), statuses);
                schedule.addSlot(
                        request,
                        ScheduleSlot.SlotType.REQUEST,
                        slotDto.arrivalTime(),
                        slotDto.serviceStartTime(),
                        slotDto.departureTime()
                );
            }

            schedule.markAsCreated();
        }
    }
}
