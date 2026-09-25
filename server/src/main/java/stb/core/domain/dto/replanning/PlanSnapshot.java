package stb.core.domain.dto.replanning;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, planner-independent representation of a schedule version.
 */
public record PlanSnapshot(int planId,
                           Integer previousPlanId,
                           LocalDate targetDate,
                           int regionId,
                           List<CrewRoute> routes,
                           List<Integer> unassignedRequestIds) {

    public PlanSnapshot {
        Objects.requireNonNull(targetDate, "targetDate must not be null");
        routes = List.copyOf(Objects.requireNonNull(routes, "routes must not be null"));
        unassignedRequestIds = List.copyOf(Objects.requireNonNull(
                unassignedRequestIds, "unassignedRequestIds must not be null"));
    }

    public record CrewRoute(int crewId, String crewName, List<Visit> visits) {

        public CrewRoute {
            Objects.requireNonNull(crewName, "crewName must not be null");
            visits = List.copyOf(Objects.requireNonNull(visits, "visits must not be null"));

            for (int index = 0; index < visits.size(); index++) {
                Visit visit = visits.get(index);
                if (visit.position() != index) {
                    throw new IllegalArgumentException(
                            "Visit position must match its route index: request " + visit.requestId());
                }

                Integer expectedPreviousRequestId = index == 0 ? null : visits.get(index - 1).requestId();
                if (!Objects.equals(visit.previousRequestId(), expectedPreviousRequestId)) {
                    throw new IllegalArgumentException(
                            "Visit predecessor must match the previous route visit: request " + visit.requestId());
                }
            }
        }
    }

    public record Visit(int requestId,
                        int position,
                        Integer previousRequestId,
                        LocalTime arrivalTime,
                        LocalTime startTime,
                        LocalTime endTime) {

        public Visit {
            if (position < 0) {
                throw new IllegalArgumentException("position must not be negative");
            }
            arrivalTime = truncateToMinutes(arrivalTime);
            startTime = truncateToMinutes(startTime);
            endTime = truncateToMinutes(endTime);
        }

        private static LocalTime truncateToMinutes(LocalTime time) {
            return time == null ? null : time.truncatedTo(ChronoUnit.MINUTES);
        }
    }
}
