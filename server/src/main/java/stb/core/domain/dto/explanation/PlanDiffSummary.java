package stb.core.domain.dto.explanation;

public record PlanDiffSummary(
        int changedRequestCount,
        int changedCrewRouteCount,
        int addedRequestCount,
        int removedRequestCount,
        int assignmentChangedRequestCount,
        int reorderedRequestCount,
        int timeChangedRequestCount
) {}
