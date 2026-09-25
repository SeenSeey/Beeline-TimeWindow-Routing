package stb.core.domain.dto.explanation;

import org.junit.jupiter.api.Test;
import stb.core.domain.dto.explanation.ReplanContext.TriggerType;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ReplanContextTest {

    @Test
    void storesSingleTriggerSubjectId() {
        ReplanContext context = new ReplanContext(
                TriggerType.CREW_BECAME_UNAVAILABLE, LocalTime.NOON, 7, 1, 2);

        assertThat(context.triggerSubjectId()).isEqualTo(7);
    }

    @Test
    void requiresTriggerTypeAndOccurrenceTime() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ReplanContext(null, LocalTime.NOON, 7, 1, 2));
        assertThatNullPointerException()
                .isThrownBy(() -> new ReplanContext(TriggerType.REQUEST_CANCELLED, null, 10, 1, 2));
    }
}
