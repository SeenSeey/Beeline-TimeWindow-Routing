package stb.core.domain.service.explanation;

import org.junit.jupiter.api.Test;
import stb.core.domain.dto.explanation.PlanDiff;
import stb.core.domain.dto.explanation.PlanDiffSummary;
import stb.core.domain.dto.explanation.PlanSnapshot;
import stb.core.domain.dto.explanation.RequestChange;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PlanDiffDomainServiceTest {

    private final PlanDiffDomainService service = new PlanDiffDomainService();

    @Test
    void comparesExplicitRequestPlacementStates() {
        PlanSnapshot previous = snapshot(
                1,
                null,
                List.of(
                        route(1, visit(10, 0, null, "09:00"), visit(20, 1, 10, "10:00")),
                        route(2, visit(30, 0, null, "11:00"))
                ),
                List.of(40)
        );
        PlanSnapshot current = snapshot(
                2,
                1,
                List.of(
                        route(1, visit(20, 0, null, "10:30")),
                        route(2, visit(10, 0, null, "09:15")),
                        route(3, visit(40, 0, null, "12:00"))
                ),
                List.of(50)
        );

        PlanDiff diff = service.compare(previous, current);

        assertThat(diff.requestChanges())
                .extracting(RequestChange::requestId, RequestChange::types)
                .containsExactly(
                        tuple(10, Set.of(RequestChange.Type.REASSIGNED, RequestChange.Type.PLANNED_TIME_CHANGED)),
                        tuple(20, Set.of(RequestChange.Type.ORDER_CHANGED, RequestChange.Type.PLANNED_TIME_CHANGED)),
                        tuple(30, Set.of(RequestChange.Type.REMOVED_FROM_PLAN)),
                        tuple(40, Set.of(RequestChange.Type.ASSIGNED_FROM_UNASSIGNED)),
                        tuple(50, Set.of(RequestChange.Type.ADDED_AS_UNASSIGNED))
                );
        assertThat(diff.summary()).isEqualTo(new PlanDiffSummary(5, 3, 1, 1, 2, 1, 2));
    }

    @Test
    void returnsEmptyDiffForEqualSnapshots() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, visit(10, 0, null, "09:00"))), List.of(20));
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(1, visit(10, 0, null, "09:00"))), List.of(20));

        PlanDiff diff = service.compare(previous, current);

        assertThat(diff.requestChanges()).isEmpty();
        assertThat(diff.crewRouteChanges()).isEmpty();
        assertThat(diff.summary()).isEqualTo(new PlanDiffSummary(0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void distinguishesNewAssignedAndUnassignedRequests() {
        PlanSnapshot previous = snapshot(1, null, List.of(), List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(1, visit(10, 0, null, "09:00"))), List.of(20));

        assertThat(service.compare(previous, current).requestChanges())
                .extracting(RequestChange::requestId, RequestChange::types)
                .containsExactly(
                        tuple(10, Set.of(RequestChange.Type.ADDED_TO_PLAN)),
                        tuple(20, Set.of(RequestChange.Type.ADDED_AS_UNASSIGNED)));
    }

    @Test
    void insertionDoesNotMarkWholeRouteAsReordered() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1,
                        visit(10, 0, null, "09:00"),
                        visit(20, 1, 10, "10:00"),
                        visit(30, 2, 20, "11:00"))),
                List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(1,
                        visit(99, 0, null, "08:30"),
                        visit(10, 1, 99, "09:00"),
                        visit(20, 2, 10, "10:00"),
                        visit(30, 3, 20, "11:00"))),
                List.of());

        PlanDiff diff = service.compare(previous, current);

        assertThat(diff.requestChanges())
                .filteredOn(change -> change.has(RequestChange.Type.ORDER_CHANGED))
                .extracting(RequestChange::requestId)
                .containsExactly(10);
    }

    @Test
    void keepsSignedTimeDeltaTogetherWithReassignment() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, visit(10, 0, null, "10:00"))), List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(2, visit(10, 0, null, "09:45"))), List.of());

        assertThat(service.compare(previous, current).requestChanges())
                .singleElement()
                .satisfies(change -> {
                    assertThat(change.types()).containsExactly(
                            RequestChange.Type.REASSIGNED,
                            RequestChange.Type.PLANNED_TIME_CHANGED);
                    assertThat(change.startTimeDeltaMinutes()).isEqualTo(-15L);
                });
    }

    @Test
    void rejectsDuplicateRequestPlacement() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, visit(10, 0, null, "09:00"))), List.of(10));
        PlanSnapshot current = snapshot(2, 1, List.of(), List.of());

        assertThatThrownBy(() -> service.compare(previous, current))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than once");
    }

    private static PlanSnapshot snapshot(int planId,
                                         Integer previousPlanId,
                                         List<PlanSnapshot.CrewRoute> routes,
                                         List<Integer> unassignedRequestIds) {
        return new PlanSnapshot(
                planId,
                previousPlanId,
                LocalDate.of(2026, 9, 25),
                1,
                routes,
                unassignedRequestIds
        );
    }

    private static PlanSnapshot.CrewRoute route(int crewId, PlanSnapshot.Visit... visits) {
        return new PlanSnapshot.CrewRoute(crewId, "Crew " + crewId, List.of(visits));
    }

    private static PlanSnapshot.Visit visit(int requestId,
                                            int position,
                                            Integer previousRequestId,
                                            String startTime) {
        LocalTime time = LocalTime.parse(startTime);
        return new PlanSnapshot.Visit(requestId, position, previousRequestId, time, time, time);
    }
}
