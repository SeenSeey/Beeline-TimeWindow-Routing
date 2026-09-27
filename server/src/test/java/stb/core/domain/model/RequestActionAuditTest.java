package stb.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RequestActionAuditTest {

    private final Request request = Request.builder().build();
    private final EngineerCrew crew = new EngineerCrew();
    private final LocalDateTime timestamp = LocalDateTime.of(2026, 9, 26, 12, 0);

    @Test
    void createsAssignmentWithRequiredStructuredDetails() {
        RequestActionAudit audit = RequestActionAudit.assigned(
                request, crew, LocalTime.of(9, 0), LocalTime.of(10, 0), timestamp);

        assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.ASSIGNED);
        assertThat(audit.getAssignedCrew()).isSameAs(crew);
        assertThat(audit.getPlannedStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(audit.getPlannedEnd()).isEqualTo(LocalTime.of(10, 0));
        assertThat(audit.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void createsTimeOnlyAndCrewChangingReschedules() {
        RequestActionAudit timeOnly = RequestActionAudit.rescheduled(
                request, null, LocalTime.of(10, 0), LocalTime.of(11, 0), timestamp);
        RequestActionAudit reassigned = RequestActionAudit.rescheduled(
                request, crew, LocalTime.of(10, 0), LocalTime.of(11, 0), timestamp);

        assertThat(timeOnly.getAssignedCrew()).isNull();
        assertThat(reassigned.getAssignedCrew()).isSameAs(crew);
    }

    @Test
    void rejectsMissingAssignmentAndPlanningData() {
        assertThatNullPointerException().isThrownBy(() -> RequestActionAudit.assigned(
                request, null, LocalTime.NOON, LocalTime.of(13, 0), timestamp));
        assertThatNullPointerException().isThrownBy(() -> RequestActionAudit.rescheduled(
                request, null, null, LocalTime.of(13, 0), timestamp));
        assertThatNullPointerException().isThrownBy(() -> RequestActionAudit.rescheduled(
                request, null, LocalTime.NOON, null, timestamp));
    }

    @Test
    void lifecycleActionsContainNoScheduleDetails() {
        assertThat(RequestActionAudit.requestCreated(request, timestamp)).satisfies(audit -> {
            assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.REQUEST_CREATED);
            assertThat(audit.getAssignedCrew()).isNull();
            assertThat(audit.getPlannedStart()).isNull();
            assertThat(audit.getPlannedEnd()).isNull();
        });
        assertThat(RequestActionAudit.started(request, timestamp).getAction())
                .isEqualTo(RequestActionAudit.Action.STARTED);
        assertThat(RequestActionAudit.completed(request, timestamp).getAction())
                .isEqualTo(RequestActionAudit.Action.COMPLETED);
        assertThat(RequestActionAudit.canceled(request, timestamp).getAction())
                .isEqualTo(RequestActionAudit.Action.CANCELED);
    }
}
