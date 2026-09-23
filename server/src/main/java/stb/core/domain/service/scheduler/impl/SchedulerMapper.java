package stb.core.domain.service.scheduler.impl;

import org.springframework.stereotype.Component;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.service.scheduler.dto.EngineerCrewDto;
import stb.core.domain.service.scheduler.dto.RequestDto;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;
import stb.core.domain.service.scheduler.impl.logic.planning.EngineerRoutingPlan;

import java.util.ArrayList;
import java.util.List;

@Component
public class SchedulerMapper {

    public EngineerCrewDto toEngineerCrewDto(EngineerCrew engineerCrew){
        return new EngineerCrewDto(engineerCrew.getId(),
                engineerCrew.getName(),
                Location.from(engineerCrew),
                engineerCrew.getShiftStartTime(),
                engineerCrew.getShiftEndTime(),
                engineerCrew.getSkills(),
                engineerCrew.getVehicle().getKeyword(),
                engineerCrew.getVehicle().getAverageSpeedKmh()
            );
    }

    public RequestDto toRequestDto(Request request) {
        return new RequestDto(
                request.getId(),
                request.getFullAddress(),
                Location.from(request),
                request.getJobType().getTotalTimeSpanMinutes(),
                request.getStartTimeWindow(),
                request.getEndTimeWindow(),
                request.getJobType().getKeyword(),
                request.getJobType().getRequiredSkills());
    }

    public ScheduleCalculationResult toSchedulerResult(EngineerRoutingPlan plan){
        List<OutputSlotDto> outputSlotDtoList = new ArrayList<>();
        for (RequestDto requestDto : plan.getRequests()){
            outputSlotDtoList.add(toOutputRequestDto(requestDto));
        }
        return new ScheduleCalculationResult(outputSlotDtoList);
    }

    public OutputSlotDto toOutputRequestDto(RequestDto r){
        Integer engId = null;
        if (r.getEngineerCrewDto()!=null){
            engId = r.getEngineerCrewDto().getId();
        }
        Integer prevId = null;
        if (r.getPreviousRequest() !=null){
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
