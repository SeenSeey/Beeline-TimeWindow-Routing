package stb.core.domain.model.replanning;

import java.time.LocalTime;
import java.util.Objects;

public record RequestChange(
        int requestId,
        Type type,
        Integer previousCrewId,
        Integer newCrewId,
        Integer previousPosition,
        Integer newPosition,
        Integer previousPredecessorRequestId,
        Integer newPredecessorRequestId,
        LocalTime previousStartTime,
        LocalTime newStartTime,
        Long startTimeDeltaMinutes
) {

    public RequestChange {
        Objects.requireNonNull(type, "type не должны быть null");
    }

    public enum Type {
        ADDED_TO_PLAN,
        ADDED_AS_UNASSIGNED,
        REMOVED_FROM_PLAN,
        ASSIGNED_FROM_UNASSIGNED,
        BECAME_UNASSIGNED,
        REASSIGNED,
        ORDER_CHANGED,
        PLANNED_TIME_CHANGED
    }

}
