package stb.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                schedule, request, AlterationReason.Type.MOVED_FOR_CREW_OFF_SHIFT, createdAt));
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
                .filter(type -> type.getCause() == cause)
                .toArray(AlterationReason.Type[]::new);
    }
}
