package stb.core.domain.dto.explanation;

import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RequestChange(
        int requestId,
        Set<Type> types,
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
        if (types.isEmpty()) {
            throw new IllegalArgumentException("types не должны быть пустыми");
        }
        types = Collections.unmodifiableSet(EnumSet.copyOf(types));
    }

    public boolean has(Type type) {
        return types.contains(type);
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
