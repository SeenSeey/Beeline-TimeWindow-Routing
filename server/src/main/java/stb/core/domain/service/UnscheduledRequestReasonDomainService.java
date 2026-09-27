package stb.core.domain.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import stb.core.domain.model.AlterationReason;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.EngineerCrewEquipment;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UnscheduledRequestReasonDomainService {

    public List<AlterationReason> analyze(Schedule schedule,
                                          List<Request> consideredRequests,
                                          List<EngineerCrew> activeCrews,
                                          boolean replanning,
                                          LocalDateTime createdAt) {
        List<ScheduleSlot> requestSlots = requestSlots(schedule);
        Set<Integer> consideredRequestIds = consideredRequests.stream()
                .map(Request::getId)
                .collect(Collectors.toSet());
        // removed-заявки получают причину перехода от AlterationReasonFactory
        Set<Integer> skippedRequestIds = new HashSet<>(requestIds(requestSlots));
        if (replanning) {
            skippedRequestIds.addAll(requestIds(requestSlots(schedule.getPreviousSchedule())));
        }
        // слоты заявок в работе и выполненных уже списали оборудование со склада, поэтому не учитываются
        Map<Integer, List<Request>> routeByCrewId = requestSlots.stream()
                .filter(slot -> consideredRequestIds.contains(slot.getRequest().getId()))
                .collect(Collectors.groupingBy(
                        slot -> slot.getEngineerCrew().getId(),
                        Collectors.mapping(ScheduleSlot::getRequest, Collectors.toList())
                ));

        return consideredRequests.stream()
                .filter(request -> !skippedRequestIds.contains(request.getId()))
                .map(request -> AlterationReason.unscheduled(
                        schedule,
                        request,
                        classify(request, activeCrews, routeByCrewId, replanning),
                        createdAt
                ))
                .toList();
    }

    private static AlterationReason.Type classify(Request request,
                                                  List<EngineerCrew> activeCrews,
                                                  Map<Integer, List<Request>> routeByCrewId,
                                                  boolean replanning) {
        List<EngineerCrew> skillCandidates = activeCrews.stream()
                .filter(crew -> hasRequiredSkill(crew, request))
                .toList();
        if (skillCandidates.isEmpty()) {
            return AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL;
        }

        List<EngineerCrew> vehicleCandidates = skillCandidates.stream()
                .filter(crew -> hasRequiredVehicle(crew, request))
                .toList();
        if (vehicleCandidates.isEmpty()) {
            return AlterationReason.Type.NO_CREW_WITH_REQUIRED_VEHICLE;
        }

        if (request.getRequiredEquipment() != null) {
            if (replanning) {
                if (vehicleCandidates.stream().noneMatch(crew ->
                        hasEquipmentInStock(crew, request, routeByCrewId.getOrDefault(crew.getId(), List.of())))) {
                    return AlterationReason.Type.NO_REQUIRED_EQUIPMENT_IN_STOCK;
                }
            } else if (vehicleCandidates.stream().noneMatch(crew ->
                    hasEquipmentCapacity(crew, routeByCrewId.getOrDefault(crew.getId(), List.of())))) {
                return AlterationReason.Type.NO_CAPACITY_FOR_REQUIRED_EQUIPMENT;
            }
        }

        return AlterationReason.Type.NO_AVAILABLE_TIME_SLOT;
    }

    private static boolean hasRequiredSkill(EngineerCrew crew, Request request) {
        return !Collections.disjoint(crew.getSkills(), request.getJobType().getRequiredSkills());
    }

    private static boolean hasRequiredVehicle(EngineerCrew crew, Request request) {
        return request.getRequiredVehicle() == null
                || request.getRequiredVehicle().getKeyword() == crew.getVehicle().getKeyword();
    }

    private static boolean hasEquipmentCapacity(EngineerCrew crew, List<Request> route) {
        long equipmentRequests = route.stream()
                .filter(request -> request.getRequiredEquipment() != null)
                .count();
        return equipmentRequests < crew.getVehicle().getEquipmentCapacity();
    }

    private static boolean hasEquipmentInStock(EngineerCrew crew, Request request, List<Request> route) {
        int equipmentId = request.getRequiredEquipment().getId();
        long equipmentRequests = route.stream()
                .filter(routeRequest -> routeRequest.getRequiredEquipment() != null)
                .filter(routeRequest -> routeRequest.getRequiredEquipment().getId() == equipmentId)
                .count();
        int stock = crew.getEquipment().stream()
                .filter(item -> item.getEquipment().getId() == equipmentId)
                .mapToInt(EngineerCrewEquipment::getQuantity)
                .sum();
        return equipmentRequests < stock;
    }

    private static Set<Integer> requestIds(List<ScheduleSlot> slots) {
        return slots.stream()
                .map(slot -> slot.getRequest().getId())
                .collect(Collectors.toSet());
    }

    private static List<ScheduleSlot> requestSlots(Schedule schedule) {
        return schedule.getSlots().stream()
                .filter(slot -> slot.getType() == ScheduleSlot.SlotType.REQUEST)
                .toList();
    }
}
