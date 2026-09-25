package stb.core.domain.dto.replanning;

import java.util.List;
import java.util.Objects;

public record CrewRouteChange(int crewId,
                              int previousRequestCount,
                              int newRequestCount,
                              List<Integer> addedRequestIds,
                              List<Integer> removedRequestIds,
                              List<Integer> reorderedRequestIds,
                              List<Integer> shiftedRequestIds) {

    public CrewRouteChange {
        addedRequestIds = copyOf(addedRequestIds, "addedRequestIds");
        removedRequestIds = copyOf(removedRequestIds, "removedRequestIds");
        reorderedRequestIds = copyOf(reorderedRequestIds, "reorderedRequestIds");
        shiftedRequestIds = copyOf(shiftedRequestIds, "shiftedRequestIds");
    }

    private static List<Integer> copyOf(List<Integer> requestIds, String fieldName) {
        return List.copyOf(Objects.requireNonNull(requestIds, fieldName + " must not be null"));
    }
}
