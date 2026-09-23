package stb.core.domain.service.scheduler.impl.logic.planning;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import lombok.*;
import stb.core.domain.service.scheduler.dto.EngineerCrewDto;
import stb.core.domain.service.scheduler.dto.RequestDto;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;
import stb.core.domain.service.scheduler.dto.TravelMatrix;

import java.util.List;

@PlanningSolution
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class EngineerRoutingPlan {

    private final TravelMatrix travelMatrix;
    private final SpeedProfile speedProfile;

    @PlanningEntityCollectionProperty
    private final List<EngineerCrewDto> engineerCrews;

    @ValueRangeProvider(id = "requestRange")
    @PlanningEntityCollectionProperty
    private final List<RequestDto> requests;

    private HardMediumSoftScore score;

    @PlanningScore
    public HardMediumSoftScore getScore() {
        return score;
    }
}