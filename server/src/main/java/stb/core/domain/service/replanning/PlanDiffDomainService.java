package stb.core.domain.service.replanning;

import stb.core.domain.model.replanning.CrewRouteChange;
import stb.core.domain.model.replanning.PlanDiff;
import stb.core.domain.model.replanning.PlanDiffSummary;
import stb.core.domain.model.replanning.PlanSnapshot;
import stb.core.domain.model.replanning.RequestChange;
import stb.core.domain.model.replanning.RequestChange.Type;

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

public class PlanDiffDomainService {

    public PlanDiff compare(PlanSnapshot previousPlan, PlanSnapshot newPlan) {
        Objects.requireNonNull(previousPlan, "previousPlan не должент быть null");
        Objects.requireNonNull(newPlan, "newPlan не должент быть null");
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
            throw new IllegalArgumentException("New plan должен содержать ссылку на previous plan");
        }
        if (!previousPlan.targetDate().equals(newPlan.targetDate())) {
            throw new IllegalArgumentException("Plan snapshots должны иметь одинаковую target date");
        }
        if (previousPlan.regionId() != newPlan.regionId()) {
            throw new IllegalArgumentException("Plan snapshots должны относиться к одному региону");
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
            putUnique(placements, new UnassignedPlacement(requestId));
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
            RequestPlacement previous = placement(previousPlacements, requestId);
            RequestPlacement current = placement(newPlacements, requestId);

            if (previous instanceof MissingPlacement) {
                changes.add(change(requestId,
                        current instanceof AssignedPlacement ? Type.ADDED_TO_PLAN : Type.ADDED_AS_UNASSIGNED,
                        previous, current));
                continue;
            }
            if (current instanceof MissingPlacement) {
                changes.add(change(requestId, Type.REMOVED_FROM_PLAN, previous, current));
                continue;
            }

            if (previous instanceof UnassignedPlacement && current instanceof AssignedPlacement) {
                changes.add(change(requestId, Type.ASSIGNED_FROM_UNASSIGNED, previous, current));
            } else if (previous instanceof AssignedPlacement && current instanceof UnassignedPlacement) {
                changes.add(change(requestId, Type.BECAME_UNASSIGNED, previous, current));
            } else if (previous instanceof AssignedPlacement previousAssigned
                    && current instanceof AssignedPlacement currentAssigned) {
                if (previousAssigned.crewId() != currentAssigned.crewId()) {
                    changes.add(change(requestId, Type.REASSIGNED, previous, current));
                } else if (!Objects.equals(
                        previousAssigned.previousRequestId(), currentAssigned.previousRequestId())) {
                    changes.add(change(requestId, Type.ORDER_CHANGED, previous, current));
                }

                if (!Objects.equals(previousAssigned.startTime(), currentAssigned.startTime())) {
                    changes.add(change(requestId, Type.PLANNED_TIME_CHANGED, previous, current));
                }
            }
        }
        return List.copyOf(changes);
    }

    private static RequestPlacement placement(Map<Integer, RequestPlacement> placements, int requestId) {
        return placements.getOrDefault(requestId, new MissingPlacement(requestId));
    }

    private static RequestChange change(int requestId,
                                        Type type,
                                        RequestPlacement previous,
                                        RequestPlacement current) {
        ChangeValues previousValues = ChangeValues.from(previous);
        ChangeValues currentValues = ChangeValues.from(current);
        LocalTime previousStart = previousValues.startTime();
        LocalTime newStart = currentValues.startTime();
        Long deltaMinutes = previousStart != null && newStart != null
                ? Duration.between(previousStart, newStart).toMinutes()
                : null;

        return new RequestChange(
                requestId,
                type,
                previousValues.crewId(),
                currentValues.crewId(),
                previousValues.position(),
                currentValues.position(),
                previousValues.previousRequestId(),
                currentValues.previousRequestId(),
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
            RequestPlacement previous = placement(previousPlacements, change.requestId());
            RequestPlacement current = placement(newPlacements, change.requestId());
            switch (change.type()) {
                case ADDED_TO_PLAN, ASSIGNED_FROM_UNASSIGNED -> routeChange(changesByCrew, current).added.add(change.requestId());
                case REMOVED_FROM_PLAN, BECAME_UNASSIGNED -> {
                    if (previous instanceof AssignedPlacement) {
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
        if (!(placement instanceof AssignedPlacement assigned)) {
            throw new IllegalArgumentException("Crew route change requires an assigned request");
        }
        return changesByCrew.get(assigned.crewId());
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
                countDistinct(requestChanges, change -> change.type() == Type.ADDED_TO_PLAN
                        || change.type() == Type.ADDED_AS_UNASSIGNED),
                countDistinct(requestChanges, change -> change.type() == Type.REMOVED_FROM_PLAN),
                countDistinct(requestChanges, change -> switch (change.type()) {
                    case ASSIGNED_FROM_UNASSIGNED, BECAME_UNASSIGNED, REASSIGNED -> true;
                    default -> false;
                }),
                countDistinct(requestChanges, change -> change.type() == Type.ORDER_CHANGED),
                countDistinct(requestChanges, change -> change.type() == Type.PLANNED_TIME_CHANGED)
        );
    }

    private static int countDistinct(List<RequestChange> changes, Predicate<RequestChange> predicate) {
        return (int) changes.stream().filter(predicate).map(RequestChange::requestId).distinct().count();
    }

    private sealed interface RequestPlacement permits AssignedPlacement, UnassignedPlacement, MissingPlacement {
        int requestId();

        static RequestPlacement assigned(int crewId, PlanSnapshot.Visit visit) {
            return new AssignedPlacement(
                    visit.requestId(),
                    crewId,
                    visit.position(),
                    visit.previousRequestId(),
                    visit.startTime()
            );
        }
    }

    private record AssignedPlacement(int requestId,
                                     int crewId,
                                     int position,
                                     Integer previousRequestId,
                                     LocalTime startTime) implements RequestPlacement {
    }

    private record UnassignedPlacement(int requestId) implements RequestPlacement {
    }

    private record MissingPlacement(int requestId) implements RequestPlacement {
    }

    private record ChangeValues(Integer crewId,
                                Integer position,
                                Integer previousRequestId,
                                LocalTime startTime) {

        private static final ChangeValues EMPTY = new ChangeValues(null, null, null, null);

        private static ChangeValues from(RequestPlacement placement) {
            if (placement instanceof AssignedPlacement assigned) {
                return new ChangeValues(
                        assigned.crewId(),
                        assigned.position(),
                        assigned.previousRequestId(),
                        assigned.startTime()
                );
            }
            return EMPTY;
        }
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
