package stb.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SlotAlterationReasonTest {

    private final ScheduleSlot slot = new ScheduleSlot();
    private final Request causeRequest = Request.builder().build();
    private final EngineerCrew causeCrew = new EngineerCrew();
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 9, 26, 12, 0);

    @Test
    void requestActionsKeepOnlyRequestCauseAndSingleSlot() {
        for (SlotAlterationReason.Action action : new SlotAlterationReason.Action[]{
                SlotAlterationReason.Action.ADDED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.MOVED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.REMOVED_AFTER_REQUEST_CANCELLATION
        }) {
            SlotAlterationReason reason = SlotAlterationReason.causedByRequest(slot, action, causeRequest, createdAt);

            assertThat(reason.getSlot()).isSameAs(slot);
            assertThat(reason.getCauseRequest()).isSameAs(causeRequest);
            assertThat(reason.getCauseCrew()).isNull();
        }
    }

    @Test
    void crewActionsKeepOnlyCrewCause() {
        SlotAlterationReason reason = SlotAlterationReason.causedByCrew(
                slot, SlotAlterationReason.Action.MOVED_FOR_CREW_OFF_SHIFT, causeCrew, createdAt);

        assertThat(reason.getCauseCrew()).isSameAs(causeCrew);
        assertThat(reason.getCauseRequest()).isNull();
    }

    @Test
    void rejectsMismatchedOrMissingCauses() {
        assertThatIllegalArgumentException().isThrownBy(() -> SlotAlterationReason.causedByRequest(
                slot, SlotAlterationReason.Action.ADDED_FOR_CREW_OFF_SHIFT, causeRequest, createdAt));
        assertThatIllegalArgumentException().isThrownBy(() -> SlotAlterationReason.causedByCrew(
                slot, SlotAlterationReason.Action.ADDED_AFTER_REQUEST_CANCELLATION, causeCrew, createdAt));
        assertThatNullPointerException().isThrownBy(() -> SlotAlterationReason.causedByRequest(
                slot, SlotAlterationReason.Action.ADDED_AFTER_REQUEST_CANCELLATION, null, createdAt));
    }
}
