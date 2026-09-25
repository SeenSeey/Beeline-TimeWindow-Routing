package stb.core.domain.dto.replanning;

import java.util.List;
import java.util.Objects;

public record PlanDiff(int previousPlanId,
                       int newPlanId,
                       List<RequestChange> requestChanges,
                       List<CrewRouteChange> crewRouteChanges,
                       PlanDiffSummary summary) {

    public PlanDiff {
        requestChanges = List.copyOf(Objects.requireNonNull(requestChanges, "requestChanges must not be null"));
        crewRouteChanges = List.copyOf(Objects.requireNonNull(crewRouteChanges, "crewRouteChanges must not be null"));
        Objects.requireNonNull(summary, "summary must not be null");
    }
}
