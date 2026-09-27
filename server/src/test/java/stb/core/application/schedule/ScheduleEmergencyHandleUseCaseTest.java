package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.application.support.reason.AlterationReasonFactory;
import stb.core.domain.model.*;
import stb.core.domain.replanning.ScheduleVersionDiff;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.AlterationReasonRepository;
import stb.core.domain.service.ScheduleRecalculationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScheduleEmergencyHandleUseCaseTest {

    @Test
    void writesAuditAndHigherPriorityReasonForAddedSlot() {
        Region region = new Region("Region", "Office", 55.7, 37.6, null);
        ReflectionTestUtils.setField(region, "id", 1);
        EngineerCrew crew = new EngineerCrew("Crew", region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", 2);
        Request emergency = Request.builder().region(region).build();
        ReflectionTestUtils.setField(emergency, "id", 3);
        Request displaced = Request.builder().region(region).build();
        ReflectionTestUtils.setField(displaced, "id", 4);
        Schedule current = Schedule.createFrom(new Schedule(LocalDate.now(), region));
        current.addSlot(displaced, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(9, 50), LocalTime.of(10, 0), LocalTime.of(11, 0));
        ScheduleSlot addedSlot = current.getSlots().getFirst();

        RequestRepository requests = mock(RequestRepository.class);
        ScheduleRecalculationService recalculation = mock(ScheduleRecalculationService.class);
        RequestActionAuditWriter auditWriter = mock(RequestActionAuditWriter.class);
        AlterationReasonRepository reasonRepository = mock(AlterationReasonRepository.class);
        when(requests.findById(3)).thenReturn(Optional.of(emergency));
        when(recalculation.recalculateSchedule(eq(region), eq(LocalDate.now()), any()))
                .thenReturn(new ScheduleRecalculationService.Recalculation(
                        current, new ScheduleVersionDiff(List.of(addedSlot), List.of(), List.of()), List.of()));

        new ScheduleEmergencyHandleUseCase(
                requests, recalculation, new RequestActionAuditFactory(), auditWriter,
                new AlterationReasonFactory(), reasonRepository).execute(3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditWriter).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).singleElement().satisfies(audit ->
                assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.ASSIGNED));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlterationReason>> reasonCaptor = ArgumentCaptor.forClass(List.class);
        verify(reasonRepository).saveAll(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).singleElement().satisfies(reason -> {
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.ADDED_FOR_HIGHER_PRIORITY_REQUEST);
            assertThat(reason.getSchedule()).isSameAs(current);
            assertThat(reason.getRequest()).isSameAs(displaced);
            assertThat(reason.getCauseRequest()).isSameAs(emergency);
        });
    }
}
