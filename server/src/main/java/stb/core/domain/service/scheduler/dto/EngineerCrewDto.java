package stb.core.domain.service.scheduler.dto;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import lombok.*;
import stb.core.domain.model.Skill;
import stb.core.domain.model.Vehicle;
import stb.core.domain.port.route.dto.Location;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PlanningEntity
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Setter
@Getter
@RequiredArgsConstructor
@ToString
public class EngineerCrewDto {


    private final int id;

    private final String name;

    private final int districtId;

    private final Location districtOfficeLocation;

    private final Location startLocation;

    private final LocalTime workDayStart;

    private final LocalTime workDayEnd;

    private List<RequestDto> route = new ArrayList<>();

    private final List<Skill> skills;

    private final Vehicle.Keyword transportTypeDto;
    private final double transportSpeed;
    private final int equipmentCapacity;
    private final Map<Integer, Integer> equipmentStock;

    private final boolean isUsed;

    @PlanningListVariable(
            valueRangeProviderRefs = "requestRange",
            allowsUnassignedValues = true
    )
    public List<RequestDto> getRoute() {
        return route;
    }
}
