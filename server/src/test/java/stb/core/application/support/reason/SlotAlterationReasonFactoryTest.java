package stb.core.application.support.reason;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;
import stb.core.domain.replanning.AutomaticReplanContext;
import stb.core.domain.replanning.ScheduleVersionDiff;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlotAlterationReasonFactoryTest {

    private final SlotAlterationReasonFactory factory = new SlotAlterationReasonFactory();
    private final Region region = new Region("Region", "Office", 0, 0, null);
    private final Schedule previous = schedule(10, null);
    private final Schedule current = schedule(11, previous);
    private final EngineerCrew crew = crew(20);
    private final Request causeRequest = request(30);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 9, 26, 12, 0);

    @Test
    void cancellationReasonsReferToCurrentSlotsForAddedAndMovedAndPreviousSlotForRemoved() {
        ScheduleSlot added = slot(current, request(1));
        ScheduleSlot movedBefore = slot(previous, request(2));
        ScheduleSlot movedAfter = slot(current, movedBefore.getRequest());
        ScheduleSlot removed = slot(previous, request(3));
        ScheduleVersionDiff diff = new ScheduleVersionDiff(
                List.of(added), List.of(removed),
                List.of(new ScheduleVersionDiff.MovedSlot(movedBefore, movedAfter)));

        List<SlotAlterationReason> reasons = factory.from(
                new AutomaticReplanContext.RequestCancellation(causeRequest), diff, createdAt);

        assertThat(reasons).extracting(SlotAlterationReason::getAction).containsExactly(
                SlotAlterationReason.Action.ADDED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.MOVED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.REMOVED_AFTER_REQUEST_CANCELLATION);
        assertThat(reasons).extracting(SlotAlterationReason::getSlot)
                .containsExactly(added, movedAfter, removed);
        assertThat(reasons).allSatisfy(reason -> {
            assertThat(reason.getCauseRequest()).isSameAs(causeRequest);
            assertThat(reason.getCauseCrew()).isNull();
            assertThat(reason.getCreatedAt()).isEqualTo(createdAt);
        });
    }

    @Test
    void supportsHigherPriorityRequestAndCrewOffShiftContexts() {
        ScheduleVersionDiff diff = new ScheduleVersionDiff(List.of(slot(current, request(1))), List.of(), List.of());

        SlotAlterationReason requestReason = factory.from(
                new AutomaticReplanContext.HigherPriorityRequest(causeRequest), diff, createdAt).getFirst();
        SlotAlterationReason crewReason = factory.from(
                new AutomaticReplanContext.CrewOffShift(crew), diff, createdAt).getFirst();

        assertThat(requestReason.getAction())
                .isEqualTo(SlotAlterationReason.Action.ADDED_FOR_HIGHER_PRIORITY_REQUEST);
        assertThat(crewReason.getAction()).isEqualTo(SlotAlterationReason.Action.ADDED_FOR_CREW_OFF_SHIFT);
        assertThat(crewReason.getCauseCrew()).isSameAs(crew);
        assertThat(crewReason.getCauseRequest()).isNull();
    }

    @Test
    void producesNothingForUnchangedOrAbsentInBothVersions() {
        assertThat(factory.from(new AutomaticReplanContext.RequestCancellation(causeRequest),
                new ScheduleVersionDiff(List.of(), List.of(), List.of()), createdAt)).isEmpty();
    }

    private Schedule schedule(int id, Schedule previousSchedule) {
        Schedule schedule = previousSchedule == null
                ? new Schedule(LocalDate.of(2026, 9, 26), region)
                : Schedule.createFrom(previousSchedule);
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private ScheduleSlot slot(Schedule schedule, Request request) {
        return new ScheduleSlot(schedule, request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(10, 0));
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
