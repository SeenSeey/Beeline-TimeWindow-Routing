package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.application.support.reason.AlterationReasonFactory;
import stb.core.domain.model.*;
import stb.core.domain.replanning.ScheduleVersionDiff;
import stb.core.domain.repository.*;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScheduleRecalculateOnRequestCancelUseCaseTest {

    @Test
    void writesAuditAndUnscheduledReasonsButNoReasonForCanceledRequestItself() {
        Region region = new Region("Region", "Office", 55.7, 37.6, null);
        ReflectionTestUtils.setField(region, "id", 1);
        EngineerCrew crew = new EngineerCrew("Crew", region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", 2);
        Request canceled = Request.builder()
                .region(region)
                .canceledAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(canceled, "id", 3);
        Request stillUnscheduled = Request.builder().region(region).build();
        ReflectionTestUtils.setField(stillUnscheduled, "id", 5);
        Schedule previous = new Schedule(LocalDate.now(), region);
        previous.addSlot(canceled, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0));
        ScheduleSlot removedSlot = previous.getSlots().getFirst();
        Schedule current = Schedule.createFrom(previous);
        AlterationReason unscheduledReason = AlterationReason.unscheduled(
                current, stillUnscheduled, AlterationReason.Type.NO_CREW_WITH_REQUIRED_SKILL, LocalDateTime.now());

        RequestRepository requests = mock(RequestRepository.class);
        ScheduleSlotRepository slots = mock(ScheduleSlotRepository.class);
        ScheduleRecalculationService recalculation = mock(ScheduleRecalculationService.class);
        RequestActionAuditWriter auditWriter = mock(RequestActionAuditWriter.class);
        AlterationReasonRepository reasonRepository = mock(AlterationReasonRepository.class);
        when(requests.findById(3)).thenReturn(Optional.of(canceled));
        when(slots.findCanceledSlotByRequestId(3)).thenReturn(Optional.of(removedSlot));
        when(recalculation.recalculateSchedule(eq(region), eq(LocalDate.now()), any()))
                .thenReturn(new ScheduleRecalculationService.Recalculation(
                        current,
                        new ScheduleVersionDiff(List.of(), List.of(removedSlot), List.of()),
                        List.of(unscheduledReason)));

        new ScheduleRecalculateOnRequestCancelUseCase(
                requests, slots, recalculation, new RequestActionAuditFactory(), auditWriter,
                new AlterationReasonFactory(), reasonRepository).execute(3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditWriter).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).singleElement().satisfies(audit ->
                assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.REMOVED_FROM_SCHEDULE));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlterationReason>> reasonCaptor = ArgumentCaptor.forClass(List.class);
        verify(reasonRepository).saveAll(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).containsExactly(unscheduledReason);
    }
}
