package stb.core.domain.dto.replanning;

import java.time.LocalTime;
import java.util.Objects;

public record RequestChange(int requestId,
                            RequestChangeType type,
                            Integer previousCrewId,
                            Integer newCrewId,
                            Integer previousPosition,
                            Integer newPosition,
                            Integer previousPredecessorRequestId,
                            Integer newPredecessorRequestId,
                            LocalTime previousStartTime,
                            LocalTime newStartTime,
                            Long startTimeDeltaMinutes) {

    public RequestChange {
        Objects.requireNonNull(type, "type must not be null");
    }
}
