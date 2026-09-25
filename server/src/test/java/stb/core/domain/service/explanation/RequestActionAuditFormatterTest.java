package stb.core.domain.service.explanation;

import org.junit.jupiter.api.Test;
import stb.core.domain.dto.explanation.PlanSnapshot;
import stb.core.domain.dto.explanation.ReplanContext;
import stb.core.domain.dto.explanation.ReplanningExplanation;
import stb.core.domain.dto.explanation.RequestChange;
import stb.core.domain.model.RequestActionAudit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestActionAuditFormatterTest {

    private final RequestActionAuditFormatter formatter = new RequestActionAuditFormatter();
    private final PlanSnapshot previous = snapshot(7, null, List.of(
            route(10, "Север", visit(42, 0, null, 9, 0)),
            route(20, "Юг", List.of())
    ));
    private final PlanSnapshot current = snapshot(7, 7, List.of(
            route(10, "Север", List.of()),
            route(20, "Юг", visit(42, 0, null, 10, 30))
    ));

    @Test
    void selectsPrimaryActionForEveryChangeType() {
        assertThat(formatter.primaryAction(change(RequestChange.Type.REASSIGNED)))
                .isEqualTo(RequestActionAudit.Action.ASSIGNED_CREW_CHANGE);
        assertThat(formatter.primaryAction(change(RequestChange.Type.ADDED_TO_PLAN)))
                .isEqualTo(RequestActionAudit.Action.INITIAL_ASSIGNMENT);
        assertThat(formatter.primaryAction(change(RequestChange.Type.ASSIGNED_FROM_UNASSIGNED)))
                .isEqualTo(RequestActionAudit.Action.INITIAL_ASSIGNMENT);
        assertThat(formatter.primaryAction(change(RequestChange.Type.BECAME_UNASSIGNED)))
                .isEqualTo(RequestActionAudit.Action.REMOVE_FROM_SCHEDULE);
        assertThat(formatter.primaryAction(change(RequestChange.Type.REMOVED_FROM_PLAN)))
                .isEqualTo(RequestActionAudit.Action.REMOVE_FROM_SCHEDULE);
        assertThat(formatter.primaryAction(change(RequestChange.Type.ORDER_CHANGED)))
                .isEqualTo(RequestActionAudit.Action.SCHEDULE_CHANGE);
        assertThat(formatter.primaryAction(change(RequestChange.Type.PLANNED_TIME_CHANGED)))
                .isEqualTo(RequestActionAudit.Action.SCHEDULE_CHANGE);
        assertThat(formatter.primaryAction(change(RequestChange.Type.ADDED_AS_UNASSIGNED)))
                .isEqualTo(RequestActionAudit.Action.REQUEST_CREATED);
    }

    @Test
    void combinesReassignmentOrderAndTimeInStableOrder() {
        RequestChange change = new RequestChange(
                42,
                EnumSet.of(RequestChange.Type.REASSIGNED,
                        RequestChange.Type.ORDER_CHANGED,
                        RequestChange.Type.PLANNED_TIME_CHANGED),
                10, 20, 1, 0, 41, null,
                LocalTime.of(9, 0), LocalTime.of(10, 30), 90L
        );

        assertThat(formatter.manual(change, previous, current)).isEqualTo(
                "После ручного изменения расписания заявка №42 была переназначена с бригады «Север» "
                        + "на бригаду «Юг»; порядок выполнения был изменён; "
                        + "плановое время было изменено с 09:00 на 10:30."
        );
    }

    @Test
    void formatsAssignmentRemovalAndAllRemainingTypes() {
        assertThat(formatter.manual(change(RequestChange.Type.ASSIGNED_FROM_UNASSIGNED), previous, current))
                .contains("была назначена бригаде «Юг» на 10:30");
        assertThat(formatter.manual(change(RequestChange.Type.BECAME_UNASSIGNED), previous, current))
                .contains("была снята с маршрута бригады «Север»");
        assertThat(formatter.manual(change(RequestChange.Type.ADDED_AS_UNASSIGNED), previous, current))
                .contains("была добавлена без назначения бригаде");
    }

    @Test
    void eventDrivenMessageAlwaysRetainsCause() {
        ReplanningExplanation.Change change = new ReplanningExplanation.Change(
                42,
                EnumSet.of(RequestChange.Type.REASSIGNED),
                "Заявка №42 переназначена на бригаду «Юг»",
                10, 20, LocalTime.of(9, 0), LocalTime.of(10, 30)
        );

        assertThat(formatter.eventDriven(
                new ReplanContext(ReplanContext.TriggerType.CREW_BECAME_UNAVAILABLE,
                        LocalTime.NOON, 10, 7, 7),
                change, previous, current
        )).isEqualTo("Из-за недоступности бригады «Север» заявка №42 переназначена на бригаду «Юг».");
    }

    @Test
    void initialAssignmentIsSelfContained() {
        assertThat(formatter.initialAssignment(42, "Юг", LocalTime.of(10, 30)))
                .isEqualTo("При первичном формировании расписания заявка №42 "
                        + "была назначена бригаде «Юг» на 10:30.");
    }

    private static RequestChange change(RequestChange.Type type) {
        return new RequestChange(
                42, EnumSet.of(type), 10, 20, 1, 0, 41, null,
                LocalTime.of(9, 0), LocalTime.of(10, 30), 90L
        );
    }

    private static PlanSnapshot snapshot(int id, Integer previousId, List<PlanSnapshot.CrewRoute> routes) {
        return new PlanSnapshot(id, previousId, LocalDate.of(2026, 9, 25), 1, routes, List.of());
    }

    private static PlanSnapshot.CrewRoute route(int id, String name, PlanSnapshot.Visit visit) {
        return route(id, name, List.of(visit));
    }

    private static PlanSnapshot.CrewRoute route(int id, String name, List<PlanSnapshot.Visit> visits) {
        return new PlanSnapshot.CrewRoute(id, name, visits);
    }

    private static PlanSnapshot.Visit visit(int id, int position, Integer previousId, int hour, int minute) {
        LocalTime time = LocalTime.of(hour, minute);
        return new PlanSnapshot.Visit(id, position, previousId, time, time, time.plusHours(1));
    }
}
