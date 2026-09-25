package stb.core.domain.dto.explanation;

import java.util.List;

public record CrewRouteChange(
        int crewId,
        int previousRequestCount,
        int newRequestCount,
        List<Integer> addedRequestIds,
        List<Integer> removedRequestIds,
        List<Integer> reorderedRequestIds,
        List<Integer> shiftedRequestIds
) {

    public CrewRouteChange {
        addedRequestIds = List.copyOf(addedRequestIds);
        removedRequestIds = List.copyOf(removedRequestIds);
        reorderedRequestIds = List.copyOf(reorderedRequestIds);
        shiftedRequestIds = List.copyOf(shiftedRequestIds);
    }
}
