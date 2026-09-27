package stb.core.application.support.reason;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Request;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.model.SlotAlterationReason;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlotAlterationReasonFormatterTest {

    private final SlotAlterationReasonFormatter formatter = new SlotAlterationReasonFormatter();

    @Test
    void formatsFromStructuredCauseWithoutAuditDependency() {
        Request cause = Request.builder().build();
        ReflectionTestUtils.setField(cause, "id", 42);
        SlotAlterationReason reason = SlotAlterationReason.causedByRequest(
                mock(ScheduleSlot.class),
                SlotAlterationReason.Action.MOVED_AFTER_REQUEST_CANCELLATION,
                cause,
                LocalDateTime.of(2026, 9, 26, 12, 0));

        assertThat(formatter.format(reason))
                .isEqualTo("Заявка перенесена после отмены заявки №42");
    }

    @Test
    void movedFromUnavailableCrewIncludesTargetCrew() {
        EngineerCrew targetCrew = mock(EngineerCrew.class);
        EngineerCrew unavailableCrew = mock(EngineerCrew.class);
        ScheduleSlot currentSlot = mock(ScheduleSlot.class);
        when(targetCrew.getName()).thenReturn("A");
        when(unavailableCrew.getName()).thenReturn("B");
        when(currentSlot.getEngineerCrew()).thenReturn(targetCrew);
        SlotAlterationReason reason = SlotAlterationReason.causedByCrew(
                currentSlot,
                SlotAlterationReason.Action.MOVED_FOR_CREW_OFF_SHIFT,
                unavailableCrew,
                LocalDateTime.of(2026, 9, 26, 12, 0));

        assertThat(formatter.format(reason))
                .isEqualTo("Заявка перераспределена на бригаду «A», так как бригада «B» недоступна");
    }
}
