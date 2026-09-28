package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.*;
import stb.core.domain.exception.DomainException;
import stb.core.domain.exception.RequestNotFoundException;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;
import stb.core.domain.service.CrewRouteTimingDomainService;
import stb.core.domain.service.CrewRouteTimingDomainService.PlannedVisit;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScheduleEditUseCase {

    private final ScheduleRepository scheduleRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final CrewRouteTimingDomainService crewRouteTimingDomainService;
    private final RequestActionAuditRepository requestActionAuditRepository;
    private final AlterationReasonRepository alterationReasonRepository;

    private record CrewPlan(EngineerCrew crew, List<PlannedVisit> visits) {
    }

    private record EditPlan(Schedule schedule,
                            List<CrewPlan> routes,
                            List<Request> removedRequests) {

        List<SchedulePreviewDto.Violation> violations() {
            List<SchedulePreviewDto.Violation> violations = new ArrayList<>();
            routes.forEach(route -> route.visits().forEach(visit -> visit.violations().forEach(message ->
                    violations.add(new SchedulePreviewDto.Violation(route.crew().getId(), visit.request().getId(), message)))));
            return violations;
        }

        Set<Integer> editedCrewIds() {
            return routes.stream().map(route -> route.crew().getId()).collect(Collectors.toSet());
        }
    }

    @Transactional(readOnly = true)
    public SchedulePreviewDto preview(int regionId, ScheduleEditDto edit) {
        EditPlan plan = plan(regionId, edit);

        List<ScheduleTimelineDto.EngineerCrewRow> rows = plan.routes().stream()
                .map(route -> ScheduleTimelineDto.EngineerCrewRow.of(route.crew(), route.visits().stream()
                        .filter(visit -> visit.startTime() != null && visit.endTime() != null)
                        .map(visit -> ScheduleTimelineDto.TimelineSlotDto.of(visit.request(), ScheduleSlot.SlotType.REQUEST,
                                visit.arrivalTime(), visit.startTime(), visit.endTime()))
                        .toList()))
                .toList();

        return new SchedulePreviewDto(rows, unassignedAfter(regionId, plan), plan.violations());
    }

    @Transactional
    public void apply(int regionId, ScheduleEditDto edit) {
        EditPlan plan = plan(regionId, edit);

        List<SchedulePreviewDto.Violation> violations = plan.violations();

        if (!violations.isEmpty()) {
            throw new DomainException.ValidationException(violations.stream()
                    .map(violation -> "заявка №%d: %s".formatted(violation.requestId(), violation.message()))
                    .collect(Collectors.joining("; ", "Изменения нельзя сохранить — ", "")));
        }

        Schedule schedule = plan.schedule();

        List<RequestStatus> statuses = requestStatusRepository.findAll();
        Set<Integer> editedCrewIds = plan.editedCrewIds();
        Map<Integer, ScheduleSlot> slotsByRequestId = schedule.getSlots()
                .stream()
                .collect(Collectors.toMap(slot -> slot.getRequest().getId(), Function.identity()));

        List<ScheduleSlot> replacedSlots = schedule.getSlots().stream()
                .filter(slot -> slot.getRequest().getAssignedCrew() != null
                        && editedCrewIds.contains(slot.getRequest().getAssignedCrew().getId()))
                .toList();

        schedule.getSlots().removeAll(replacedSlots);
        scheduleSlotRepository.deleteAll(replacedSlots);

        List<RequestActionAudit> audit = new ArrayList<>();

        for (Request request : plan.removedRequests()) {
            audit.add(RequestActionAudit.removedFromSchedule(request));
            request.unassign(statuses);
        }

        plan.routes().forEach(route -> {
            EngineerCrew crew = route.crew();

            for (PlannedVisit visit : route.visits()) {
                Request request = visit.request();
                EngineerCrew previousCrew = request.getAssignedCrew();

                if (previousCrew == null) {
                    request.assignCrew(crew, statuses);
                    audit.add(RequestActionAudit.assigned(request, crew, visit.startTime(), visit.endTime()));
                } else if (previousCrew.getId() != crew.getId()) {
                    request.assignCrew(crew, statuses);
                    audit.add(RequestActionAudit.reassigned(request, crew, visit.startTime(), visit.endTime()));
                } else if (slotsByRequestId.containsKey(request.getId())) {
                    ScheduleSlot previousSlot = slotsByRequestId.get(request.getId());

                    if (!Objects.equals(previousSlot.getStartTimeWindow(), visit.startTime())) {
                        audit.add(RequestActionAudit.rescheduled(request, visit.startTime(), visit.endTime()));
                    }
                }

                schedule.addSlot(request, crew, ScheduleSlot.SlotType.REQUEST, visit.arrivalTime(), visit.startTime(), visit.endTime());
            }
        });

        // the dispatcher's changes aren't explained by the last recalculation anymore
        List<Integer> changedRequestIds = audit.stream().map(item -> item.getRequest().getId()).toList();
        alterationReasonRepository.deleteAllByScheduleIdAndRequestIdIn(schedule.getId(), changedRequestIds);

        scheduleRepository.save(schedule);
        requestActionAuditRepository.saveAll(audit);
    }

    private EditPlan plan(int regionId, ScheduleEditDto edit) {
        LocalDate today = LocalDate.now();
        Schedule schedule = scheduleRepository
                .findByTargetDateAndRegionId(today, regionId)
                .orElseThrow(() -> new ScheduleNotFoundException("Расписание на сегодня для региона ещё не сформировано"));
        if (schedule.getStatus() != Schedule.Status.CREATED) {
            throw new DomainException.ConflictException("Расписание ещё формируется — изменить его пока нельзя");
        }
        if (edit == null || edit.routes() == null) {
            throw new DomainException.ValidationException("Не переданы маршруты бригад");
        }

        Map<Integer, EngineerCrew> regionCrews = engineerCrewRepository.findAllActiveByRegionIdAndTargetDate(regionId, today).stream()
                .collect(Collectors.toMap(EngineerCrew::getId, Function.identity()));

        Map<Integer, EngineerCrew> editedCrews = new LinkedHashMap<>();
        for (ScheduleEditDto.CrewRoute route : edit.routes()) {
            EngineerCrew crew = regionCrews.get(route.crewId());
            if (crew == null) {
                throw new DomainException.ValidationException("Бригада №%d не относится к этому региону".formatted(route.crewId()));
            }
            if (editedCrews.put(crew.getId(), crew) != null) {
                throw new DomainException.ValidationException("Маршрут бригады №%d передан дважды".formatted(crew.getId()));
            }
            if (route.stops() == null) {
                throw new DomainException.ValidationException("Не передан состав маршрута бригады №%d".formatted(crew.getId()));
            }
        }

        List<Integer> allRequestIds = edit.routes().stream()
                .flatMap(route -> stopsOf(route).stream())
                .map(ScheduleEditDto.Stop::requestId)
                .toList();
        Map<Integer, Request> requestsById = requestRepository.findAllById(allRequestIds).stream()
                .collect(Collectors.toMap(Request::getId, Function.identity()));

        Set<Integer> plannedRequestIds = new HashSet<>();
        List<CrewPlan> routes = new ArrayList<>();
        for (ScheduleEditDto.CrewRoute route : edit.routes()) {
            List<CrewRouteTimingDomainService.Stop> stopsInOrder = new ArrayList<>();
            for (ScheduleEditDto.Stop stop : stopsOf(route)) {
                int requestId = stop.requestId();
                Request request = requestsById.get(requestId);
                if (request == null) {
                    throw new RequestNotFoundException("Заявка №%d не найдена".formatted(requestId));
                }
                checkCanBePlanned(request, regionId, editedCrews.keySet(), route.crewId());
                if (!plannedRequestIds.add(requestId)) {
                    throw new DomainException.ValidationException("Заявка №%d стоит в маршрутах дважды".formatted(requestId));
                }
                stopsInOrder.add(new CrewRouteTimingDomainService.Stop(request, stop.startTime()));
            }
            EngineerCrew crew = editedCrews.get(route.crewId());
            routes.add(new CrewPlan(crew, crewRouteTimingDomainService.planRoute(crew, stopsInOrder)));
        }

        List<Request> removedRequests = schedule.getSlots().stream()
                .filter(slot -> slot.getEngineerCrew() != null
                        && editedCrews.containsKey(slot.getEngineerCrew().getId())
                        && !plannedRequestIds.contains(slot.getRequest().getId()))
                .map(ScheduleSlot::getRequest)
                .distinct()
                .toList();

        return new EditPlan(schedule, routes, removedRequests);
    }

    private static List<ScheduleEditDto.Stop> stopsOf(ScheduleEditDto.CrewRoute route) {
        return Optional.ofNullable(route.stops()).orElse(List.of());
    }

    private static void checkCanBePlanned(Request request, int regionId, Set<Integer> editedCrewIds, int routeCrewId) {
        if (request.getRegion().getId() != regionId) {
            throw new DomainException.ValidationException("Заявка №%d из другого региона".formatted(request.getId()));
        }
        EngineerCrew currentCrew = request.getAssignedCrew();
        if (request.isCanceled()) {
            throw new DomainException.ValidationException("Заявка №%d отменена".formatted(request.getId()));
        }
        if ((request.isCompleted() || request.isInProgress()) && (currentCrew == null || currentCrew.getId() != routeCrewId)) {
            throw new DomainException.ValidationException("Заявка №%d уже в работе или выполнена — перенести её к другой бригаде нельзя".formatted(request.getId()));
        }
        if (currentCrew != null && !editedCrewIds.contains(currentCrew.getId())) {
            throw new DomainException.ValidationException(
                    "Заявка №%d назначена бригаде «%s» — передайте и её маршрут".formatted(request.getId(), currentCrew.getName()));
        }
        if (currentCrew == null && request.getStatus() != null
                && request.getStatus().getKeyword() != RequestStatus.Keyword.IDLE) {
            throw new DomainException.ValidationException("Заявку №%d нельзя поставить в расписание".formatted(request.getId()));
        }
    }

    private List<UnassignedRequestDto> unassignedAfter(int regionId, EditPlan plan) {
        Set<Integer> planned = plan.routes().stream()
                .flatMap(route -> route.visits().stream())
                .map(visit -> visit.request().getId())
                .collect(Collectors.toSet());

        List<UnassignedRequestDto> unassigned = new ArrayList<>();
        requestRepository.findAllUnassignedByRegionIdAndStatus(regionId, RequestStatus.Keyword.IDLE).stream()
                .filter(request -> !planned.contains(request.getId()))
                .map(UnassignedRequestDto::from)
                .forEach(unassigned::add);
        plan.removedRequests().stream().map(UnassignedRequestDto::from).forEach(unassigned::add);
        return unassigned;
    }
}
