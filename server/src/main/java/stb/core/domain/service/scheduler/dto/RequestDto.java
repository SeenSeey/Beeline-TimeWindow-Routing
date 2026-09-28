package stb.core.domain.service.scheduler.dto;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Skill;
import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.dto.Location;
import stb.core.domain.service.scheduler.impl.logic.planning.EngineerRoutingPlan;
import stb.core.domain.service.scheduler.impl.logic.planning.TravelTimeCalculator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@PlanningEntity
@Setter
@Getter
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class RequestDto {

    private final int id;
    private final String description;

    private final Location location;

    private final int districtId;

    private final int serviceDurationMinutes;

    private final LocalTime windowStart;
    private final LocalTime windowEnd;
    private final JobType.Keyword jobTypeKeyword;
    private final List<Skill> requiredSkills;
    private final Vehicle.Keyword requiredTransportTypeDto;
    private final boolean isUrgent;
    private final Integer requiredEquipmentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime deadline;
    private final Integer previousCrewId;
    //Shadow vars

    private EngineerCrewDto engineerCrewDto;

    private RequestDto previousRequest;

    private LocalTime arrivalTime;




    //Shadow vars

    @JsonIgnore
    @InverseRelationShadowVariable(sourceVariableName = "route")
    public EngineerCrewDto getEngineerCrewDto() {
        return engineerCrewDto;
    }
    @JsonIgnore
    @PreviousElementShadowVariable(sourceVariableName = "route")
    public RequestDto getPreviousRequest() {
        return previousRequest;
    }
    @ShadowVariable(supplierName = "computeArrivalTime")
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }


    @ShadowSources({
            "engineerCrewDto",
            "previousRequest.arrivalTime"
    })
    public LocalTime computeArrivalTime(
            EngineerRoutingPlan solution
    ) {

        if (engineerCrewDto == null) {
            return null;
        }
        if (previousRequest == null) {
            return TravelTimeCalculator.calculateArrival(
                    engineerCrewDto.getStartLocation(),
                    location,
                    engineerCrewDto.getTransportSpeed(),
                    engineerCrewDto.getWorkDayStart(),
                    solution.getTravelMatrix(),
                    solution.getSpeedProfile()
            );
        }

        LocalTime previousDeparture =
                previousRequest.getDepartureTime();
        if (previousDeparture == null) {
            return null;
        }

        return TravelTimeCalculator.calculateArrival(
                previousRequest.getLocation(),
                location,
                engineerCrewDto.getTransportSpeed(),
                previousDeparture,
                solution.getTravelMatrix(),
                solution.getSpeedProfile()
        );
    }


    public LocalTime getServiceStartTime() {

        if (arrivalTime == null) {
            return null;
        }

        if (arrivalTime.isBefore(windowStart)) {
            return windowStart;
        }

        return arrivalTime;
    }

    public LocalTime getDepartureTime() {

        LocalTime serviceStartTime = getServiceStartTime();

        if (serviceStartTime == null) {
            return null;
        }
        LocalTime result = serviceStartTime.plusMinutes(
                serviceDurationMinutes
        );
        if (!result.isAfter(serviceStartTime)){
            return null;
        }
        return result;
    }
}