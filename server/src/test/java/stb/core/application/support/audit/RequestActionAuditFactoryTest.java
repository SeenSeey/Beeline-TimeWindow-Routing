package stb.core.application.support.audit;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;
import stb.core.domain.replanning.ScheduleVersionDiff;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestActionAuditFactoryTest {

    private final RequestActionAuditFactory factory = new RequestActionAuditFactory();
    private final Region region = new Region("Region", "Office", 0, 0, null);
    private final Schedule previous = new Schedule(LocalDate.of(2026, 9, 26), region);
    private final Schedule current = Schedule.createFrom(previous);
    private final EngineerCrew oldCrew = crew(1);
    private final EngineerCrew newCrew = crew(2);
    private final LocalDateTime timestamp = LocalDateTime.of(2026, 9, 26, 12, 0);

    @Test
    void projectsAddedMovedAndRemovedIntoIndependentAuditActions() {
        ScheduleSlot added = slot(current, request(1), newCrew, 9, 10);
        ScheduleSlot removed = slot(previous, request(2), oldCrew, 10, 11);
        Request timeMovedRequest = request(3);
        ScheduleSlot timeBefore = slot(previous, timeMovedRequest, oldCrew, 11, 12);
        ScheduleSlot timeAfter = slot(current, timeMovedRequest, oldCrew, 12, 13);
        Request reassignedRequest = request(4);
        ScheduleSlot crewBefore = slot(previous, reassignedRequest, oldCrew, 13, 14);
        ScheduleSlot crewAfter = slot(current, reassignedRequest, newCrew, 13, 14);

        List<RequestActionAudit> audits = factory.from(new ScheduleVersionDiff(
                List.of(added), List.of(removed), List.of(
                new ScheduleVersionDiff.MovedSlot(timeBefore, timeAfter),
                new ScheduleVersionDiff.MovedSlot(crewBefore, crewAfter))), timestamp);

        assertThat(audits).extracting(RequestActionAudit::getAction).containsExactly(
                RequestActionAudit.Action.ASSIGNED,
                RequestActionAudit.Action.REMOVED_FROM_SCHEDULE,
                RequestActionAudit.Action.RESCHEDULED,
                RequestActionAudit.Action.RESCHEDULED);
        assertThat(audits.get(2).getAssignedCrew()).isNull();
        assertThat(audits.get(3).getAssignedCrew()).isSameAs(newCrew);
        assertThat(audits).allSatisfy(audit -> assertThat(audit.getTimestamp()).isEqualTo(timestamp));
    }

    private ScheduleSlot slot(Schedule schedule, Request request, EngineerCrew crew, int start, int end) {
        return new ScheduleSlot(schedule, request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(start, 0), LocalTime.of(start, 0), LocalTime.of(end, 0));
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
