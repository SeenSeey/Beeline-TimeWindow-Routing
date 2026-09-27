package stb.core.application.support.reason;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiffDomainService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlterationReasonFactoryTest {

    private final AlterationReasonFactory factory = new AlterationReasonFactory();
    private final Region region = new Region("Region", "Office", 0, 0, null);
    private final Schedule previous = schedule(10, null);
    private final Schedule current = schedule(11, previous);
    private final EngineerCrew crew = crew(20);
    private final EngineerCrew otherCrew = crew(21);
    private final EngineerCrew offShiftCrew = crew(22);
    private final Request causeRequest = request(30);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 9, 26, 12, 0);

    @Test
    void addedAndRemovedRequestsAreExplainedByRecalculationContext() {
        Request added = request(1);
        Request removed = request(2);
        slot(previous, removed, crew, 9);
        slot(current, added, crew, 9);

        List<AlterationReason> reasons = from(new AutomaticReplanContext.RequestCancellation(causeRequest));

        assertThat(reasons).extracting(AlterationReason::getType).containsExactly(
                AlterationReason.Type.ADDED_AFTER_REQUEST_CANCELLATION,
                AlterationReason.Type.REMOVED_AFTER_REQUEST_CANCELLATION);
        assertThat(reasons).extracting(AlterationReason::getRequest).containsExactly(added, removed);
        assertThat(reasons).allSatisfy(reason -> {
            assertThat(reason.getSchedule()).isSameAs(current);
            assertThat(reason.getCauseRequest()).isSameAs(causeRequest);
            assertThat(reason.getCreatedAt()).isEqualTo(createdAt);
        });
    }

    @Test
    void supportsHigherPriorityRequestAndCrewOffShiftContexts() {
        slot(current, request(1), crew, 9);

        AlterationReason requestReason = from(new AutomaticReplanContext.HigherPriorityRequest(causeRequest)).getFirst();
        AlterationReason crewReason = from(new AutomaticReplanContext.CrewOffShift(offShiftCrew)).getFirst();

        assertThat(requestReason.getType()).isEqualTo(AlterationReason.Type.ADDED_FOR_HIGHER_PRIORITY_REQUEST);
        assertThat(requestReason.getCauseRequest()).isSameAs(causeRequest);
        assertThat(crewReason.getType()).isEqualTo(AlterationReason.Type.ADDED_FOR_CREW_OFF_SHIFT);
        assertThat(crewReason.getCauseCrew()).isSameAs(offShiftCrew);
        assertThat(crewReason.getCauseRequest()).isNull();
    }

    @Test
    void skipsReasonForCauseRequestItself() {
        Request other = request(1);
        slot(previous, causeRequest, crew, 9);
        slot(current, other, otherCrew, 9);

        assertThat(from(new AutomaticReplanContext.RequestCancellation(causeRequest)))
                .extracting(AlterationReason::getRequest)
                .containsExactly(other);
    }

    @Test
    void requestOfOffShiftCrewIsReassignedBecauseOfThatCrew() {
        Request request = request(1);
        slot(previous, request, offShiftCrew, 9);
        slot(current, request, crew, 9);

        assertThat(from(new AutomaticReplanContext.CrewOffShift(offShiftCrew))).singleElement().satisfies(reason -> {
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.REASSIGNED_FOR_CREW_OFF_SHIFT);
            assertThat(reason.getCauseCrew()).isSameAs(offShiftCrew);
            assertThat(reason.getSchedule()).isSameAs(current);
        });
    }

    @Test
    void cascadeReassignmentNamesPreviousCrewInsteadOfRecalculationContext() {
        Request request = request(1);
        slot(previous, request, otherCrew, 9);
        slot(current, request, crew, 9);

        assertThat(from(new AutomaticReplanContext.CrewOffShift(offShiftCrew))).singleElement().satisfies(reason -> {
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.REASSIGNED);
            assertThat(reason.getPreviousCrew()).isSameAs(otherCrew);
            assertThat(reason.getCauseCrew()).isNull();
        });
    }

    @Test
    void laterVisitIsExplainedByNearestRequestInsertedBeforeIt() {
        Request first = request(1);
        Request farInserted = request(2);
        Request nearInserted = request(3);
        Request shifted = request(4);
        slot(previous, first, crew, 9);
        slot(previous, shifted, crew, 10);
        slot(current, first, crew, 9);
        slot(current, farInserted, crew, 10);
        slot(current, nearInserted, crew, 11);
        slot(current, shifted, crew, 12);

        assertThat(reasonFor(shifted, new AutomaticReplanContext.CrewOffShift(offShiftCrew))).satisfies(reason -> {
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.RESCHEDULED_AFTER_INSERTION);
            assertThat(reason.getCauseRequest()).isSameAs(nearInserted);
            assertThat(reason.getPreviousStartTime()).isEqualTo(LocalTime.of(10, 0));
        });
    }

    @Test
    void emergencyInsertedBeforeRequestIsNamedAndGetsNoReasonItself() {
        Request shifted = request(1);
        slot(previous, shifted, crew, 9);
        slot(current, causeRequest, crew, 9);
        slot(current, shifted, crew, 10);

        assertThat(from(new AutomaticReplanContext.HigherPriorityRequest(causeRequest))).singleElement().satisfies(reason -> {
            assertThat(reason.getRequest()).isSameAs(shifted);
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.RESCHEDULED_AFTER_INSERTION);
            assertThat(reason.getCauseRequest()).isSameAs(causeRequest);
        });
    }

    @Test
    void earlierVisitIsExplainedByRequestRemovedBeforeIt() {
        Request shifted = request(1);
        slot(previous, causeRequest, crew, 9);
        slot(previous, shifted, crew, 10);
        slot(current, shifted, crew, 9);

        assertThat(from(new AutomaticReplanContext.RequestCancellation(causeRequest))).singleElement().satisfies(reason -> {
            assertThat(reason.getRequest()).isSameAs(shifted);
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.RESCHEDULED_AFTER_REMOVAL);
            assertThat(reason.getCauseRequest()).isSameAs(causeRequest);
        });
    }

    @Test
    void unchangedStartIsNotExplainedAsEarlierVisit() {
        Request shifted = request(1);
        slot(previous, causeRequest, crew, 9);
        slot(previous, shifted, crew, 10);
        current.addSlot(shifted, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(9, 50), LocalTime.of(10, 0), LocalTime.of(11, 10));

        assertThat(reasonFor(shifted, new AutomaticReplanContext.RequestCancellation(causeRequest)).getType())
                .isEqualTo(AlterationReason.Type.RESCHEDULED);
    }

    @Test
    void reorderOrRetimingWithoutRouteChangeIsPlainReschedule() {
        Request reordered = request(1);
        Request retimed = request(2);
        slot(previous, retimed, crew, 9);
        slot(previous, reordered, crew, 10);
        slot(current, reordered, crew, 11);
        slot(current, retimed, crew, 12);

        assertThat(from(new AutomaticReplanContext.RequestCancellation(causeRequest)))
                .extracting(AlterationReason::getType)
                .containsOnly(AlterationReason.Type.RESCHEDULED);
    }

    private AlterationReason reasonFor(Request request, AutomaticReplanContext context) {
        return from(context).stream()
                .filter(reason -> reason.getRequest() == request)
                .findFirst()
                .orElseThrow();
    }

    private List<AlterationReason> from(AutomaticReplanContext context) {
        return factory.from(context, current,
                new ScheduleVersionDiffDomainService().compare(previous, current), createdAt);
    }

    private Schedule schedule(int id, Schedule previousSchedule) {
        Schedule schedule = previousSchedule == null
                ? new Schedule(LocalDate.of(2026, 9, 26), region)
                : Schedule.createFrom(previousSchedule);
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private static void slot(Schedule schedule, Request request, EngineerCrew crew, int startHour) {
        schedule.addSlot(request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(startHour, 0).minusMinutes(10), LocalTime.of(startHour, 0), LocalTime.of(startHour, 50));
    }

    private Request request(int id) {
        Request request = Request.builder().build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private EngineerCrew crew(int id) {
        EngineerCrew crew = new EngineerCrew("Crew " + id, region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", id);
        return crew;
    }
}
