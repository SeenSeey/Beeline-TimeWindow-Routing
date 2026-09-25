package stb.core.domain.dto.replanning;

import java.time.LocalTime;

/**
 * A request location inside one immutable plan snapshot.
 */
public record RequestPlacement(int requestId,
                               Integer crewId,
                               Integer position,
                               Integer previousRequestId,
                               LocalTime arrivalTime,
                               LocalTime startTime,
                               LocalTime endTime) {

    public static RequestPlacement assigned(int crewId, PlanSnapshot.Visit visit) {
        return new RequestPlacement(
                visit.requestId(),
                crewId,
                visit.position(),
                visit.previousRequestId(),
                visit.arrivalTime(),
                visit.startTime(),
                visit.endTime()
        );
    }

    public static RequestPlacement unassigned(int requestId) {
        return new RequestPlacement(requestId, null, null, null, null, null, null);
    }

    public boolean assigned() {
        return crewId != null;
    }
}
