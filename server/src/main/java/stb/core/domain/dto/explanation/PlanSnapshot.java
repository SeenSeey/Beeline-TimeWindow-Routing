package stb.core.domain.dto.explanation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public record PlanSnapshot(
        int planId,
        Integer previousPlanId,
        LocalDate targetDate,
        int regionId,
        List<CrewRoute> routes,
        List<Integer> unassignedRequestIds
) {

    public PlanSnapshot {
        Objects.requireNonNull(targetDate, "targetDate не должна быть null");
        routes = List.copyOf(routes);
        unassignedRequestIds = List.copyOf(unassignedRequestIds);
    }

    public record CrewRoute(int crewId, String crewName, List<Visit> visits) {

        public CrewRoute {
            Objects.requireNonNull(crewName, "crewName не должно быть null");
            visits = List.copyOf(visits);

            for (int index = 0; index < visits.size(); index++) {
                Visit visit = visits.get(index);
                if (visit.position() != index) {
                    throw new IllegalArgumentException(
                            "Visit position должна соответствовать route index: request " + visit.requestId());
                }

                Integer expectedPreviousRequestId = index == 0 ? null : visits.get(index - 1).requestId();
                if (!Objects.equals(visit.previousRequestId(), expectedPreviousRequestId)) {
                    throw new IllegalArgumentException(
                            "Visit predecessor должен соответствовать previous route visit: request " + visit.requestId());
                }
            }
        }
    }

    public record Visit(
            int requestId,
            int position,
            Integer previousRequestId,
            LocalTime arrivalTime,
            LocalTime startTime,
            LocalTime endTime
    ) {

        public Visit {
            if (position < 0) {
                throw new IllegalArgumentException("position не должна быть отрицательной");
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
