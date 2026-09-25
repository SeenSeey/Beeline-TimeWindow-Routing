package stb.core.domain.model.replanning;

public record PlanDiffSummary(
        int changedRequestCount,
        int changedCrewRouteCount,
        int addedRequestCount,
        int removedRequestCount,
        int assignmentChangedRequestCount,
        int reorderedRequestCount,
        int timeChangedRequestCount
) {}
