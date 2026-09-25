package stb.core.domain.service.explanation;

import org.junit.jupiter.api.Test;
import stb.core.domain.dto.explanation.PlanDiff;
import stb.core.domain.dto.explanation.PlanSnapshot;
import stb.core.domain.dto.explanation.ReplanContext;
import stb.core.domain.dto.explanation.ReplanContext.TriggerType;
import stb.core.domain.dto.explanation.ReplanningExplanation;
import stb.core.domain.dto.explanation.RequestChange;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplanningExplanationDomainServiceTest {

    private final PlanDiffDomainService diffService = new PlanDiffDomainService();
    private final ReplanningExplanationDomainService explanationService = new ReplanningExplanationDomainService();

    @Test
    void explainsEmergencyRequestAndGroupsChangesByRequest() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, "Соколов", visit(10, 0, null, "09:00"))), List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(1, "Соколов",
                        visit(99, 0, null, "09:00"),
                        visit(10, 1, 99, "10:00"))), List.of());

        ReplanningExplanation explanation = explain(
                context(TriggerType.EMERGENCY_REQUEST_CREATED, 99), previous, current);

        assertThat(explanation.title()).isEqualTo("План перестроен из-за аварийной заявки");
        assertThat(explanation.summary())
                .isEqualTo("Аварийная заявка №99 назначена бригаде «Соколов». Изменено других заявок: 1.");
        assertThat(explanation.counters()).isEqualTo(new ReplanningExplanation.Counters(1, 0, 0, 1, 1));
        assertThat(explanation.changes()).hasSize(2);
        assertThat(explanation.changes().getFirst()).satisfies(change -> {
            assertThat(change.requestId()).isEqualTo(99);
            assertThat(change.changeTypes()).containsExactly(RequestChange.Type.ADDED_TO_PLAN);
            assertThat(change.message())
                    .isEqualTo("Аварийная заявка №99 добавлена в маршрут бригады «Соколов» на 09:00");
        });
        assertThat(explanation.changes().getLast().changeTypes())
                .containsExactly(RequestChange.Type.ORDER_CHANGED, RequestChange.Type.PLANNED_TIME_CHANGED);
        assertThat(explanation.changes().getLast().message())
                .isEqualTo("Порядок выполнения заявки №10 изменён; начало заявки №10 сдвинуто с 09:00 на 10:00");
    }

    @Test
    void explainsCancellationSeparatelyFromNewAssignments() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, "Соколов",
                        visit(10, 0, null, "09:00"),
                        visit(20, 1, 10, "10:00"))),
                List.of(30));
        PlanSnapshot current = snapshot(2, 1,
                List.of(
                        route(1, "Соколов", visit(20, 0, null, "09:30")),
                        route(2, "Волков", visit(30, 0, null, "11:00"))),
                List.of());

        ReplanningExplanation explanation = explain(
                context(TriggerType.REQUEST_CANCELLED, 10), previous, current);

        assertThat(explanation.title()).isEqualTo("План перестроен после отмены заявки");
        assertThat(explanation.summary())
                .isEqualTo("Отменённая заявка №10 удалена из плана. Изменено других заявок: 2.");
        assertThat(explanation.changes().getFirst().message())
                .isEqualTo("Отменённая заявка №10 удалена из маршрута бригады «Соколов»");
        assertThat(explanation.counters()).isEqualTo(new ReplanningExplanation.Counters(1, 0, 0, 1, 1));
        assertThat(explanation.changes())
                .filteredOn(change -> change.requestId() == 30)
                .singleElement()
                .satisfies(change -> assertThat(change.message()).contains("ранее остававшаяся без исполнителя"));
    }

    @Test
    void putsUnavailableCrewRequestsBeforeOtherChanges() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(
                        route(7, "Орлов", visit(1, 0, null, "09:00"), visit(2, 1, 1, "10:00")),
                        route(8, "Волков", visit(3, 0, null, "11:00"))),
                List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(8, "Волков", visit(1, 0, null, "09:30"), visit(3, 1, 1, "11:00"))),
                List.of(2));

        ReplanningExplanation explanation = explain(
                context(TriggerType.CREW_BECAME_UNAVAILABLE, 7), previous, current);

        assertThat(explanation.title()).isEqualTo("План перестроен из-за недоступности бригады");
        assertThat(explanation.summary())
                .isEqualTo("Бригада «Орлов» стала недоступна. Заявок из её маршрута изменено: 2.");
        assertThat(explanation.changes()).extracting(ReplanningExplanation.Change::requestId)
                .containsExactly(1, 2, 3);
        assertThat(explanation.changes()).extracting(ReplanningExplanation.Change::message)
                .containsExactly(
                        "Заявка №1 переназначена бригаде «Волков»; начало заявки №1 сдвинуто с 09:00 на 09:30",
                        "Заявка №2 осталась без исполнителя",
                        "Порядок выполнения заявки №3 изменён"
                );
    }

    @Test
    void returnsExplicitMessageForEmptyDiff() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, "Соколов", visit(10, 0, null, "09:00"))), List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(1, "Соколов", visit(10, 0, null, "09:00"))), List.of());

        ReplanningExplanation explanation = explain(
                context(TriggerType.REQUEST_CANCELLED, 99), previous, current);

        assertThat(explanation.summary())
                .isEqualTo("План перестроен после отмены заявки №99. Другие назначения и порядок заявок не изменились.");
        assertThat(explanation.changes()).isEmpty();
        assertThat(explanation.counters()).isEqualTo(new ReplanningExplanation.Counters(0, 0, 0, 0, 0));
    }

    @Test
    void doesNotClaimThatCancellationRemovedAnAlreadyAbsentRequest() {
        PlanSnapshot previous = snapshot(1, null,
                List.of(route(1, "Соколов", visit(10, 0, null, "09:00"))), List.of());
        PlanSnapshot current = snapshot(2, 1,
                List.of(route(1, "Соколов", visit(10, 0, null, "10:00"))), List.of());

        ReplanningExplanation explanation = explain(
                context(TriggerType.REQUEST_CANCELLED, 99), previous, current);

        assertThat(explanation.summary())
                .startsWith("План перестроен после отмены заявки №99.")
                .doesNotContain("удалена");
    }

    @Test
    void rejectsDataFromDifferentPlanVersions() {
        PlanSnapshot previous = snapshot(1, null, List.of(), List.of());
        PlanSnapshot current = snapshot(2, 1, List.of(), List.of());
        PlanDiff diff = diffService.compare(previous, current);
        ReplanContext wrongContext = new ReplanContext(
                TriggerType.REQUEST_CANCELLED, LocalTime.NOON, 10, 1, 3);

        assertThatThrownBy(() -> explanationService.explain(wrongContext, diff, previous, current))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("одной паре версий");
    }

    @Test
    void generatedTextsDoNotClaimOptimizerInternals() {
        PlanSnapshot previous = snapshot(1, null, List.of(), List.of());
        PlanSnapshot current = snapshot(2, 1, List.of(), List.of(99));

        ReplanningExplanation explanation = explain(
                context(TriggerType.EMERGENCY_REQUEST_CREATED, 99), previous, current);

        String text = explanation.title() + " " + explanation.summary() + " "
                + explanation.changes().getFirst().message();
        assertThat(text).doesNotContainIgnoringCase("оптималь", "штраф", "score", "вытесн");
    }

    private ReplanningExplanation explain(ReplanContext context,
                                          PlanSnapshot previous,
                                          PlanSnapshot current) {
        PlanDiff diff = diffService.compare(previous, current);
        return explanationService.explain(context, diff, previous, current);
    }

    private static ReplanContext context(TriggerType type, int triggerSubjectId) {
        return new ReplanContext(type, LocalTime.of(14, 0), triggerSubjectId, 1, 2);
    }

    private static PlanSnapshot snapshot(int planId,
                                         Integer previousPlanId,
                                         List<PlanSnapshot.CrewRoute> routes,
                                         List<Integer> unassignedRequestIds) {
        return new PlanSnapshot(planId, previousPlanId, LocalDate.of(2026, 9, 25), 1, routes, unassignedRequestIds);
    }

    private static PlanSnapshot.CrewRoute route(int crewId,
                                                String crewName,
                                                PlanSnapshot.Visit... visits) {
        return new PlanSnapshot.CrewRoute(crewId, crewName, List.of(visits));
    }

    private static PlanSnapshot.Visit visit(int requestId,
                                            int position,
                                            Integer previousRequestId,
                                            String startTime) {
        LocalTime time = LocalTime.parse(startTime);
        return new PlanSnapshot.Visit(requestId, position, previousRequestId, time, time, time);
    }
}
