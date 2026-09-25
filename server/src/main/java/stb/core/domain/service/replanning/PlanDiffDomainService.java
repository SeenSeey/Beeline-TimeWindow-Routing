package stb.core.domain.service.replanning;

import stb.core.domain.dto.replanning.CrewRouteChange;
import stb.core.domain.dto.replanning.PlanDiff;
import stb.core.domain.dto.replanning.PlanDiffSummary;
import stb.core.domain.dto.replanning.PlanSnapshot;
import stb.core.domain.dto.replanning.RequestChange;
import stb.core.domain.dto.replanning.RequestChangeType;
import stb.core.domain.dto.replanning.RequestPlacement;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Compares immutable plan snapshots and reports observable business changes.
 * The service is deliberately independent of persistence and the scheduling engine.
 */
public class PlanDiffDomainService {

    public PlanDiff compare(PlanSnapshot previousPlan, PlanSnapshot newPlan) {
        Objects.requireNonNull(previousPlan, "previousPlan must not be null");
        Objects.requireNonNull(newPlan, "newPlan must not be null");
        validateComparable(previousPlan, newPlan);

        Map<Integer, RequestPlacement> previousPlacements = index(previousPlan);
        Map<Integer, RequestPlacement> newPlacements = index(newPlan);
        List<RequestChange> requestChanges = findRequestChanges(previousPlacements, newPlacements);
        List<CrewRouteChange> crewRouteChanges = findCrewRouteChanges(
                previousPlan, newPlan, previousPlacements, newPlacements, requestChanges);

        return new PlanDiff(
                previousPlan.planId(),
                newPlan.planId(),
                requestChanges,
                crewRouteChanges,
                summarize(requestChanges, crewRouteChanges)
        );
    }

    private static void validateComparable(PlanSnapshot previousPlan, PlanSnapshot newPlan) {
        if (!Objects.equals(newPlan.previousPlanId(), previousPlan.planId())) {
            throw new IllegalArgumentException("New plan must reference the previous plan");
        }
        if (!previousPlan.targetDate().equals(newPlan.targetDate())) {
            throw new IllegalArgumentException("Plan snapshots must have the same target date");
        }
        if (previousPlan.regionId() != newPlan.regionId()) {
            throw new IllegalArgumentException("Plan snapshots must belong to the same region");
        }
    }

    private static Map<Integer, RequestPlacement> index(PlanSnapshot snapshot) {
        Map<Integer, RequestPlacement> placements = new HashMap<>();
        Set<Integer> crewIds = new TreeSet<>();

        for (PlanSnapshot.CrewRoute route : snapshot.routes()) {
            if (!crewIds.add(route.crewId())) {
                throw new IllegalArgumentException("Duplicate crew route: " + route.crewId());
            }
            for (PlanSnapshot.Visit visit : route.visits()) {
                putUnique(placements, RequestPlacement.assigned(route.crewId(), visit));
            }
        }
        for (Integer requestId : snapshot.unassignedRequestIds()) {
            if (requestId == null) {
                throw new IllegalArgumentException("Unassigned request id must not be null");
            }
            putUnique(placements, RequestPlacement.unassigned(requestId));
        }
        return placements;
    }

    private static void putUnique(Map<Integer, RequestPlacement> placements, RequestPlacement placement) {
        if (placements.putIfAbsent(placement.requestId(), placement) != null) {
            throw new IllegalArgumentException("Request occurs more than once in a plan snapshot: " + placement.requestId());
        }
    }

    private static List<RequestChange> findRequestChanges(Map<Integer, RequestPlacement> previousPlacements,
                                                           Map<Integer, RequestPlacement> newPlacements) {
        Set<Integer> requestIds = new TreeSet<>(previousPlacements.keySet());
        requestIds.addAll(newPlacements.keySet());

        List<RequestChange> changes = new ArrayList<>();
        for (int requestId : requestIds) {
            RequestPlacement previous = previousPlacements.get(requestId);
            RequestPlacement current = newPlacements.get(requestId);

            if (previous == null) {
                changes.add(change(requestId,
                        current.assigned() ? RequestChangeType.ADDED_TO_PLAN : RequestChangeType.ADDED_AS_UNASSIGNED,
                        null, current));
                continue;
            }
            if (current == null) {
                changes.add(change(requestId, RequestChangeType.REMOVED_FROM_PLAN, previous, null));
                continue;
            }

            if (!previous.assigned() && current.assigned()) {
                changes.add(change(requestId, RequestChangeType.ASSIGNED_FROM_UNASSIGNED, previous, current));
            } else if (previous.assigned() && !current.assigned()) {
                changes.add(change(requestId, RequestChangeType.BECAME_UNASSIGNED, previous, current));
            } else if (previous.assigned() && !Objects.equals(previous.crewId(), current.crewId())) {
                changes.add(change(requestId, RequestChangeType.REASSIGNED, previous, current));
            } else if (previous.assigned()
                    && !Objects.equals(previous.previousRequestId(), current.previousRequestId())) {
                changes.add(change(requestId, RequestChangeType.ORDER_CHANGED, previous, current));
            }

            if (previous.assigned() && current.assigned()
                    && !Objects.equals(previous.startTime(), current.startTime())) {
                changes.add(change(requestId, RequestChangeType.PLANNED_TIME_CHANGED, previous, current));
            }
        }
        return List.copyOf(changes);
    }

    private static RequestChange change(int requestId,
                                        RequestChangeType type,
                                        RequestPlacement previous,
                                        RequestPlacement current) {
        LocalTime previousStart = previous == null ? null : previous.startTime();
        LocalTime newStart = current == null ? null : current.startTime();
        Long deltaMinutes = previousStart != null && newStart != null
                ? Duration.between(previousStart, newStart).toMinutes()
                : null;

        return new RequestChange(
                requestId,
                type,
                previous == null ? null : previous.crewId(),
                current == null ? null : current.crewId(),
                previous == null ? null : previous.position(),
                current == null ? null : current.position(),
                previous == null ? null : previous.previousRequestId(),
                current == null ? null : current.previousRequestId(),
                previousStart,
                newStart,
                deltaMinutes
        );
    }

    private static List<CrewRouteChange> findCrewRouteChanges(PlanSnapshot previousPlan,
                                                               PlanSnapshot newPlan,
                                                               Map<Integer, RequestPlacement> previousPlacements,
                                                               Map<Integer, RequestPlacement> newPlacements,
                                                               List<RequestChange> requestChanges) {
        Map<Integer, Integer> previousCounts = requestCountsByCrew(previousPlan);
        Map<Integer, Integer> newCounts = requestCountsByCrew(newPlan);
        Map<Integer, MutableCrewRouteChange> changesByCrew = new LinkedHashMap<>();

        Set<Integer> crewIds = new TreeSet<>(previousCounts.keySet());
        crewIds.addAll(newCounts.keySet());
        for (int crewId : crewIds) {
            changesByCrew.put(crewId, new MutableCrewRouteChange(
                    crewId, previousCounts.getOrDefault(crewId, 0), newCounts.getOrDefault(crewId, 0)));
        }

        for (RequestChange change : requestChanges) {
            RequestPlacement previous = previousPlacements.get(change.requestId());
            RequestPlacement current = newPlacements.get(change.requestId());
            switch (change.type()) {
                case ADDED_TO_PLAN, ASSIGNED_FROM_UNASSIGNED -> routeChange(changesByCrew, current).added.add(change.requestId());
                case REMOVED_FROM_PLAN, BECAME_UNASSIGNED -> {
                    if (previous != null && previous.assigned()) {
                        routeChange(changesByCrew, previous).removed.add(change.requestId());
                    }
                }
                case REASSIGNED -> {
                    routeChange(changesByCrew, previous).removed.add(change.requestId());
                    routeChange(changesByCrew, current).added.add(change.requestId());
                }
                case ORDER_CHANGED -> routeChange(changesByCrew, current).reordered.add(change.requestId());
                case PLANNED_TIME_CHANGED -> routeChange(changesByCrew, current).shifted.add(change.requestId());
                case ADDED_AS_UNASSIGNED -> {
                    // No crew route is affected.
                }
            }
        }

        return changesByCrew.values().stream()
                .filter(MutableCrewRouteChange::changed)
                .map(MutableCrewRouteChange::toImmutable)
                .toList();
    }

    private static MutableCrewRouteChange routeChange(Map<Integer, MutableCrewRouteChange> changesByCrew,
                                                       RequestPlacement placement) {
        return changesByCrew.get(Objects.requireNonNull(placement.crewId()));
    }

    private static Map<Integer, Integer> requestCountsByCrew(PlanSnapshot snapshot) {
        return snapshot.routes().stream().collect(Collectors.toMap(
                PlanSnapshot.CrewRoute::crewId,
                route -> route.visits().size()
        ));
    }

    private static PlanDiffSummary summarize(List<RequestChange> requestChanges,
                                             List<CrewRouteChange> crewRouteChanges) {
        int changedRequestCount = (int) requestChanges.stream().map(RequestChange::requestId).distinct().count();
        return new PlanDiffSummary(
                changedRequestCount,
                crewRouteChanges.size(),
                countDistinct(requestChanges, change -> change.type() == RequestChangeType.ADDED_TO_PLAN
                        || change.type() == RequestChangeType.ADDED_AS_UNASSIGNED),
                countDistinct(requestChanges, change -> change.type() == RequestChangeType.REMOVED_FROM_PLAN),
                countDistinct(requestChanges, change -> switch (change.type()) {
                    case ASSIGNED_FROM_UNASSIGNED, BECAME_UNASSIGNED, REASSIGNED -> true;
                    default -> false;
                }),
                countDistinct(requestChanges, change -> change.type() == RequestChangeType.ORDER_CHANGED),
                countDistinct(requestChanges, change -> change.type() == RequestChangeType.PLANNED_TIME_CHANGED)
        );
    }

    private static int countDistinct(List<RequestChange> changes, Predicate<RequestChange> predicate) {
        return (int) changes.stream().filter(predicate).map(RequestChange::requestId).distinct().count();
    }

    private static final class MutableCrewRouteChange {
        private final int crewId;
        private final int previousRequestCount;
        private final int newRequestCount;
        private final Set<Integer> added = new TreeSet<>();
        private final Set<Integer> removed = new TreeSet<>();
        private final Set<Integer> reordered = new TreeSet<>();
        private final Set<Integer> shifted = new TreeSet<>();

        private MutableCrewRouteChange(int crewId, int previousRequestCount, int newRequestCount) {
            this.crewId = crewId;
            this.previousRequestCount = previousRequestCount;
            this.newRequestCount = newRequestCount;
        }

        private boolean changed() {
            return previousRequestCount != newRequestCount
                    || !added.isEmpty()
                    || !removed.isEmpty()
                    || !reordered.isEmpty()
                    || !shifted.isEmpty();
        }

        private CrewRouteChange toImmutable() {
            return new CrewRouteChange(
                    crewId,
                    previousRequestCount,
                    newRequestCount,
                    List.copyOf(added),
                    List.copyOf(removed),
                    List.copyOf(reordered),
                    List.copyOf(shifted)
            );
        }
    }
}
