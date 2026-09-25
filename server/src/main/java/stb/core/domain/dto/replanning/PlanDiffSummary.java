package stb.core.domain.dto.replanning;

public record PlanDiffSummary(int changedRequestCount,
                              int changedCrewRouteCount,
                              int addedRequestCount,
                              int removedRequestCount,
                              int assignmentChangedRequestCount,
                              int reorderedRequestCount,
                              int timeChangedRequestCount) {
}
