package stb.core.domain.service.scheduler.impl;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.EngineerCrewEquipment;
import stb.core.domain.model.Request;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.repository.projection.CrewLatestSlot;
import stb.core.domain.service.scheduler.dto.EngineerCrewDto;
import stb.core.domain.service.scheduler.dto.RequestDto;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;
import stb.core.domain.service.scheduler.impl.logic.planning.EngineerRoutingPlan;

@Component
public class SchedulerMapper {

    public EngineerCrewDto toEngineerCrewDto(
        EngineerCrew engineerCrew,
        Map<Integer, CrewLatestSlot> crewLatestSlots,
        Map<Integer, Integer> slotsCountByCrewId,
        boolean replanning
    ) {
        Location location;
        LocalTime shiftStartTime;

        if (crewLatestSlots != null && crewLatestSlots.containsKey(engineerCrew.getId())) {
            CrewLatestSlot latestSlot = crewLatestSlots.get(engineerCrew.getId());

            location = Location.from(latestSlot);
            shiftStartTime = latestSlot.getEndTimeWindow();
        } else {
            location = Location.from(engineerCrew);
            shiftStartTime = engineerCrew.getShiftStartTime();
        }

        return new EngineerCrewDto(
            engineerCrew.getId(),
            engineerCrew.getName(),
            engineerCrew.getRegion().getId(),
            Location.from(engineerCrew),
            location,
            shiftStartTime,
            engineerCrew.getShiftEndTime(),
            engineerCrew.getSkills(),
            engineerCrew.getVehicle().getKeyword(),
            engineerCrew.getVehicle().getAverageSpeedKmh(),
            engineerCrew.getVehicle().getEquipmentCapacity(),
            replanning ? toEquipmentStock(engineerCrew.getEquipment()) : null,
            replanning && slotsCountByCrewId.containsKey(engineerCrew.getId())
        );
    }

    private static Map<Integer, Integer> toEquipmentStock(List<EngineerCrewEquipment> stock) {
        return stock
            .stream()
            .collect(Collectors.toMap(item -> item.getEquipment().getId(), EngineerCrewEquipment::getQuantity));
    }

    public RequestDto toRequestDto(Request request) {
        return new RequestDto(
            request.getId(),
            request.getFullAddress(),
            Location.from(request),
            request.getRegion().getId(),
            request.getJobType().getTotalTimeSpanMinutes() - request.getJobType().getMaxTravelTimeSpanMinutes(),
            request.getStartTimeWindow(),
            request.getEndTimeWindow(),
            request.getJobType().getKeyword(),
            request.getJobType().getRequiredSkills(),
            request.getRequiredVehicle() != null ? request.getRequiredVehicle().getKeyword() : null,
            request.isUrgent(),
            request.getRequiredEquipment() != null ? request.getRequiredEquipment().getId() : null,
            request.getCreatedAt(),
            request.getDeadline()
        );
    }

    public ScheduleCalculationResult toSchedulerResult(EngineerRoutingPlan plan) {
        List<OutputSlotDto> outputSlotDtoList = new ArrayList<>();
        for (RequestDto requestDto : plan.getRequests()) {
            outputSlotDtoList.add(toOutputRequestDto(requestDto));
        }
        return new ScheduleCalculationResult(outputSlotDtoList);
    }

    public OutputSlotDto toOutputRequestDto(RequestDto r) {
        Integer engId = null;
        if (r.getEngineerCrewDto() != null) {
            engId = r.getEngineerCrewDto().getId();
        }
        Integer prevId = null;
        if (r.getPreviousRequest() != null) {
            prevId = r.getPreviousRequest().getId();
        }
        return new OutputSlotDto(
            r.getId(),
            engId,
            r.getServiceDurationMinutes(),
            r.getWindowStart(),
            r.getWindowEnd(),
            r.getArrivalTime(),
            r.getServiceStartTime(),
            r.getDepartureTime(),
            prevId
        );
    }
}
