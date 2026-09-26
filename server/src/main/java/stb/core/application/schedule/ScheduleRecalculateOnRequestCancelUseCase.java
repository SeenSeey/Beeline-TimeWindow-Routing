package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.repository.*;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.service.RouteDomainService;
import stb.core.domain.service.SpeedProfileDomainService;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.ScheduleContext;
import stb.core.domain.service.scheduler.dto.TravelMatrix;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleRecalculateOnRequestCancelUseCase {

    private final RequestRepository requestRepository;
    private final RouteDomainService routeDomainService;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final SpeedProfileDomainService speedProfileDomainService;
    private final ScheduleAssemblerDomainService assemblerDomainService;
    private final RequestStatusRepository requestStatusRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void execute(int requestId) {
        LocalDate targetDate = LocalDate.now();

        log.info("Пересчёт расписания на {} в связи с отменой заявки {}", targetDate, requestId);

        Request canceledRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Заявка %s не существует".formatted(requestId)));

        Region region = canceledRequest.getRegion();
        ScheduleSlot latestSlot = scheduleSlotRepository.findCanceledSlotByRequestId(requestId)
                .orElse(null);

        if (latestSlot == null) {
            log.info("Отменена заявка, которая не была включена в расписание, отмена перепланирования");
            return;
        }

        Schedule currentSchedule = latestSlot.getSchedule();

        List<Request> requestsToReschedule = requestRepository.findAllForRescheduleByRegionId(region.getId());
        List<EngineerCrew> engineerCrews = engineerCrewRepository.findAllActiveByRegionIdAndTargetDate(region.getId(), targetDate);
        List<CrewLatestSlot> crewLatestSlots = scheduleSlotRepository.findLatestSlotsForCrewsByRegionId(region.getId());

        crewLatestSlots.forEach(slot -> log.info("Координаты из базы: {} {}", slot.getLatitude(), slot.getLongitude()));

        List<ScheduleSlot> staticSlots = new ArrayList<>();

        for (ScheduleSlot slot : currentSchedule.getSlots()) {
            Request request = slot.getRequest();

            if (request.isInProgress() || request.isCompleted()) {
                staticSlots.add(slot);
            }
        }

        List<Location> points = Stream.concat(Stream.concat(
                Stream.of(Location.from(region)),
                requestsToReschedule.stream().map(Location::from)),
                crewLatestSlots.stream().map(Location::from)
        ).toList();

        ScheduleContext context = new ScheduleContext();
        context.setRequests(requestsToReschedule);
        context.setEngineerCrews(engineerCrews);
        context.setTravelMatrix(new TravelMatrix(routeDomainService.calculateAllRoutes(points, Vehicle.Keyword.CAR)));
        context.setSpeedProfile(speedProfileDomainService.createSpeedProfile());
        context.setCrewsLatestSlots(crewLatestSlots);
        context.setReplanning(true);

        ScheduleCalculationResult result = assemblerDomainService.createSchedule(context);

        Map<Integer, Request> requestById = requestsToReschedule.stream().collect(Collectors.toMap(Request::getId, Function.identity()));
        Map<Integer, EngineerCrew> crewById = engineerCrews.stream().collect(Collectors.toMap(EngineerCrew::getId, Function.identity()));

        List<RequestStatus> statuses = requestStatusRepository.findAll();

        Schedule schedule = Schedule.createFrom(currentSchedule);
        List<Request> updatedRequests = new ArrayList<>();

        for (ScheduleSlot staticSlot : staticSlots) {
            schedule.addSlot(staticSlot);
        }

        for (OutputSlotDto slotDto : result.requestDtoList()) {
            // TODO: отмечать историю?
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

            updatedRequests.add(request);
        }

        scheduleRepository.save(schedule);
        requestRepository.saveAll(updatedRequests);

        log.info("Расписание для региона {} пересчитано в связи с отменой заявки {}", region.getDisplayName(), canceledRequest.getId());
    }
}
