package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.application.support.reason.SlotAlterationReasonFactory;
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
import static org.mockito.Mockito.*;

class ScheduleRecalculateOnCrewOfShiftUseCaseTest {

    @Test
    void writesAuditAndReasonWhenRequestMovesToActiveCrew() {
        Region region = new Region("Region", "Office", 55.7, 37.6, null);
        ReflectionTestUtils.setField(region, "id", 1);
        EngineerCrew unavailableCrew = crew(2, "Unavailable", region);
        EngineerCrew activeCrew = crew(3, "Active", region);
        Request request = Request.builder()
                .region(region)
                .assignedCrew(unavailableCrew)
                .assignedAt(LocalDateTime.now().minusHours(1))
                .build();
        ReflectionTestUtils.setField(request, "id", 4);
        Schedule previous = new Schedule(LocalDate.now(), region);
        previous.addSlot(request, unavailableCrew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0));
        Schedule current = Schedule.createFrom(previous);
        current.addSlot(request, activeCrew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(9, 50), LocalTime.of(10, 0), LocalTime.of(11, 0));

        RequestRepository requests = mock(RequestRepository.class);
        RequestStatusRepository statuses = mock(RequestStatusRepository.class);
        EngineerCrewRepository crews = mock(EngineerCrewRepository.class);
        ScheduleRecalculationService recalculation = mock(ScheduleRecalculationService.class);
        RequestActionAuditWriter auditWriter = mock(RequestActionAuditWriter.class);
        SlotAlterationReasonRepository reasonRepository = mock(SlotAlterationReasonRepository.class);
        when(requests.findAllIncomingByAssignedCrewId(2)).thenReturn(List.of(request));
        when(crews.findById(2)).thenReturn(Optional.of(unavailableCrew));
        when(statuses.findAll()).thenReturn(List.of(new RequestStatus("Idle", RequestStatus.Keyword.IDLE)));
        when(recalculation.recalculateSchedule(region, LocalDate.now())).thenReturn(new ScheduleVersionDiff(
                List.of(), List.of(), List.of(new ScheduleVersionDiff.MovedSlot(
                        previous.getSlots().getFirst(), current.getSlots().getFirst()))));

        new ScheduleRecalculateOnCrewOfShiftUseCase(
                requests, statuses, crews, recalculation, new RequestActionAuditFactory(), auditWriter,
                new SlotAlterationReasonFactory(), reasonRepository).execute(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditWriter).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).singleElement().satisfies(audit -> {
            assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.RESCHEDULED);
            assertThat(audit.getAssignedCrew()).isSameAs(activeCrew);
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SlotAlterationReason>> reasonCaptor = ArgumentCaptor.forClass(List.class);
        verify(reasonRepository).saveAll(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).singleElement().satisfies(reason -> {
            assertThat(reason.getAction()).isEqualTo(SlotAlterationReason.Action.MOVED_FOR_CREW_OFF_SHIFT);
            assertThat(reason.getSlot().getSchedule()).isSameAs(current);
            assertThat(reason.getCauseCrew()).isSameAs(unavailableCrew);
        });
    }

    private static EngineerCrew crew(int id, String name, Region region) {
        EngineerCrew crew = new EngineerCrew(name, region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", id);
        return crew;
    }
}
