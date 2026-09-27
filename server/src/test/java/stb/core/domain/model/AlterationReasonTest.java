package stb.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AlterationReasonTest {

    private final Schedule schedule = new Schedule(LocalDate.of(2026, 9, 27), new Region());
    private final Request request = Request.builder().build();
    private final Request causeRequest = Request.builder().build();
    private final EngineerCrew causeCrew = new EngineerCrew();
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 12, 0);
    private final EngineerCrew previousCrew = new EngineerCrew();
    private final ScheduleSlot previousSlot = new ScheduleSlot(schedule, request, previousCrew,
            ScheduleSlot.SlotType.REQUEST, LocalTime.of(9, 20), LocalTime.of(9, 30), LocalTime.of(10, 30));

    @Test
    void requestTypesKeepOnlyRequestCause() {
        for (AlterationReason.Type type : typesWith(AlterationReason.Cause.REQUEST)) {
            AlterationReason reason = AlterationReason.causedByRequest(
                    schedule, request, type, causeRequest, createdAt);

            assertThat(reason.getSchedule()).isSameAs(schedule);
            assertThat(reason.getRequest()).isSameAs(request);
            assertThat(reason.getType()).isEqualTo(type);
            assertThat(reason.getCauseRequest()).isSameAs(causeRequest);
            assertThat(reason.getCauseCrew()).isNull();
            assertThat(reason.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Test
    void crewTypesKeepOnlyCrewCause() {
        for (AlterationReason.Type type : typesWith(AlterationReason.Cause.CREW)) {
            AlterationReason reason = AlterationReason.causedByCrew(
                    schedule, request, type, causeCrew, createdAt);

            assertThat(reason.getCauseCrew()).isSameAs(causeCrew);
            assertThat(reason.getCauseRequest()).isNull();
        }
    }

    @Test
    void unscheduledTypesKeepNoCause() {
        for (AlterationReason.Type type : typesWith(AlterationReason.Cause.NONE)) {
            AlterationReason reason = AlterationReason.unscheduled(schedule, request, type, createdAt);

            assertThat(reason.getCauseRequest()).isNull();
            assertThat(reason.getCauseCrew()).isNull();
        }
    }

    @Test
    void movementTypesKeepPreviousPositionOfRequest() {
        AlterationReason byRequest = AlterationReason.movedByRequest(
                schedule, previousSlot, AlterationReason.Type.RESCHEDULED_AFTER_INSERTION, causeRequest, createdAt);
        AlterationReason withoutCause = AlterationReason.moved(
                schedule, previousSlot, AlterationReason.Type.REASSIGNED, createdAt);

        assertThat(byRequest.getRequest()).isSameAs(request);
        assertThat(byRequest.getCauseRequest()).isSameAs(causeRequest);
        assertThat(byRequest.getPreviousCrew()).isSameAs(previousCrew);
        assertThat(byRequest.getPreviousStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(withoutCause.getCauseRequest()).isNull();
        assertThat(withoutCause.getCauseCrew()).isNull();
        assertThat(withoutCause.getPreviousCrew()).isSameAs(previousCrew);
    }

    @Test
    void previousPositionIsRequiredOnlyForMovementTypes() {
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.causedByRequest(
                schedule, request, AlterationReason.Type.RESCHEDULED_AFTER_INSERTION, causeRequest, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.unscheduled(
                schedule, request, AlterationReason.Type.RESCHEDULED, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.moved(
                schedule, previousSlot, AlterationReason.Type.NO_AVAILABLE_TIME_SLOT, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.movedByRequest(
                schedule, previousSlot, AlterationReason.Type.ADDED_AFTER_REQUEST_CANCELLATION, causeRequest, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.moved(
                schedule, previousSlot, AlterationReason.Type.RESCHEDULED_AFTER_REMOVAL, createdAt));
    }

    @Test
    void rejectsTypeWithMismatchedCause() {
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.causedByRequest(
                schedule, request, AlterationReason.Type.ADDED_FOR_CREW_OFF_SHIFT, causeRequest, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.causedByRequest(
                schedule, request, AlterationReason.Type.NO_AVAILABLE_TIME_SLOT, causeRequest, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.causedByCrew(
                schedule, request, AlterationReason.Type.ADDED_AFTER_REQUEST_CANCELLATION, causeCrew, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.causedByCrew(
                schedule, request, AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL, causeCrew, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.unscheduled(
                schedule, request, AlterationReason.Type.REMOVED_AFTER_REQUEST_CANCELLATION, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> AlterationReason.unscheduled(
                schedule, request, AlterationReason.Type.REASSIGNED_FOR_CREW_OFF_SHIFT, createdAt));
    }

    @Test
    void rejectsMissingRequiredFields() {
        AlterationReason.Type type = AlterationReason.Type.NO_AVAILABLE_TIME_SLOT;

        assertThatNullPointerException().isThrownBy(() ->
                AlterationReason.unscheduled(null, request, type, createdAt));
        assertThatNullPointerException().isThrownBy(() ->
                AlterationReason.unscheduled(schedule, null, type, createdAt));
        assertThatNullPointerException().isThrownBy(() ->
                AlterationReason.unscheduled(schedule, request, null, createdAt));
        assertThatNullPointerException().isThrownBy(() ->
                AlterationReason.unscheduled(schedule, request, type, null));
        assertThatNullPointerException().isThrownBy(() -> AlterationReason.causedByRequest(
                schedule, request, AlterationReason.Type.ADDED_AFTER_REQUEST_CANCELLATION, null, createdAt));
        assertThatNullPointerException().isThrownBy(() -> AlterationReason.causedByCrew(
                schedule, request, AlterationReason.Type.ADDED_FOR_CREW_OFF_SHIFT, null, createdAt));
    }

    private static AlterationReason.Type[] typesWith(AlterationReason.Cause cause) {
        return Arrays.stream(AlterationReason.Type.values())
                .filter(type -> type.getCause() == cause && !type.isMovement())
                .toArray(AlterationReason.Type[]::new);
    }
}
