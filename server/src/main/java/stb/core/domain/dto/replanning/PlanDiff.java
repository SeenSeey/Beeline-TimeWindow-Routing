package stb.core.domain.dto.replanning;

import java.util.List;
import java.util.Objects;

public record PlanDiff(
        int previousPlanId,
        int newPlanId,
        List<RequestChange> requestChanges,
        List<CrewRouteChange> crewRouteChanges,
        PlanDiffSummary summary
) {

    public PlanDiff {
        requestChanges = List.copyOf(requestChanges);
        crewRouteChanges = List.copyOf(crewRouteChanges);
        Objects.requireNonNull(summary, "summary не должно быть null");
    }

}
