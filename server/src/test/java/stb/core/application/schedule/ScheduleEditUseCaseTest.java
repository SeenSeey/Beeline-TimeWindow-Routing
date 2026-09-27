package stb.core.application.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.audit.RequestActionAuditFactory;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.domain.dto.ScheduleEditDto;
import stb.core.domain.exception.DomainException;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.service.CrewRouteTimingDomainService;
import stb.core.domain.replanning.ScheduleVersionDiffDomainService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleEditUseCaseTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final EngineerCrewRepository crewRepository = mock(EngineerCrewRepository.class);
    private final RequestRepository requestRepository = mock(RequestRepository.class);
    private final RequestStatusRepository statusRepository = mock(RequestStatusRepository.class);
    private final CrewRouteTimingDomainService timingService = mock(CrewRouteTimingDomainService.class);
    private final RequestActionAuditWriter auditWriter = mock(RequestActionAuditWriter.class);

    private ScheduleEditUseCase useCase;
    private EngineerCrew crew;
    private Request request;
    private Schedule schedule;
    private ScheduleEditDto edit;

    @BeforeEach
    void setUp() {
        Region region = new Region("Регион", "Офис", 55.7, 37.6, null);
        ReflectionTestUtils.setField(region, "id", 1);
        crew = new EngineerCrew(
                "Бригада 1", region, null, LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", 10);
        RequestStatus assigned = new RequestStatus("Назначена", RequestStatus.Keyword.ASSIGNED);
        request = Request.builder().region(region).assignedCrew(crew).status(assigned).build();
        ReflectionTestUtils.setField(request, "id", 42);
        schedule = new Schedule(LocalDate.now(), region);
        ReflectionTestUtils.setField(schedule, "id", 7);
        schedule.markAsCreated();
        schedule.addSlot(request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0));
        edit = new ScheduleEditDto(List.of(
                new ScheduleEditDto.CrewRoute(10, List.of(
                        new ScheduleEditDto.Stop(42, LocalTime.of(10, 30))))
        ));

        when(scheduleRepository.findByTargetDateAndRegionId(LocalDate.now(), 1))
                .thenReturn(Optional.of(schedule));
        when(crewRepository.findAllActiveByRegionIdAndTargetDate(1, LocalDate.now())).thenReturn(List.of(crew));
        when(requestRepository.findAllById(List.of(42))).thenReturn(List.of(request));
        when(requestRepository.findAllUnassignedByRegionIdAndStatus(1, RequestStatus.Keyword.IDLE))
                .thenReturn(List.of());
        when(statusRepository.findAll()).thenReturn(List.of(assigned));

        useCase = new ScheduleEditUseCase(
                scheduleRepository, crewRepository, requestRepository, statusRepository,
                timingService, new ScheduleVersionDiffDomainService(), new RequestActionAuditFactory(), auditWriter);
    }

    @Test
    void createsOneAuditForTimeOnlyChange() {
        when(timingService.planRoute(any(), any())).thenReturn(List.of(
                new CrewRouteTimingDomainService.PlannedVisit(
                        request, LocalTime.of(10, 20), LocalTime.of(10, 30), LocalTime.of(11, 30), List.of())
        ));

        useCase.apply(1, edit);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditWriter).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(audit -> {
            assertThat(audit.getRequest()).isSameAs(request);
            assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.RESCHEDULED);
            assertThat(audit.getAssignedCrew()).isNull();
            assertThat(audit.getPlannedStart()).isEqualTo(LocalTime.of(10, 30));
            assertThat(audit.getPlannedEnd()).isEqualTo(LocalTime.of(11, 30));
            assertThat(audit.getTimestamp()).isNotNull();
        });
        assertThat(schedule.getSlots()).singleElement().satisfies(slot ->
                assertThat(slot.getStartTimeWindow()).isEqualTo(LocalTime.of(9, 0)));
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getPreviousSchedule()).isSameAs(schedule);
        assertThat(scheduleCaptor.getValue().getSlots()).singleElement().satisfies(slot ->
                assertThat(slot.getStartTimeWindow()).isEqualTo(LocalTime.of(10, 30)));
    }

    @Test
    void identicalEditDoesNotCreateScheduleVersionOrAudit() {
        when(timingService.planRoute(any(), any())).thenReturn(List.of(
                new CrewRouteTimingDomainService.PlannedVisit(
                        request, LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0), List.of())
        ));
        ScheduleEditDto identicalEdit = new ScheduleEditDto(List.of(
                new ScheduleEditDto.CrewRoute(10, List.of(
                        new ScheduleEditDto.Stop(42, LocalTime.of(9, 0))))
        ));

        useCase.apply(1, identicalEdit);

        verify(scheduleRepository, never()).save(any());
        verify(auditWriter, never()).saveAll(any());
        verify(statusRepository, never()).findAll();
    }

    @Test
    void validationErrorDoesNotWriteAuditOrMutateSchedule() {
        when(timingService.planRoute(any(), any())).thenReturn(List.of(
                new CrewRouteTimingDomainService.PlannedVisit(
                        request, LocalTime.of(10, 20), LocalTime.of(10, 30), LocalTime.of(11, 30),
                        List.of("время вне окна"))
        ));

        assertThatThrownBy(() -> useCase.apply(1, edit))
                .isInstanceOf(DomainException.ValidationException.class);

        verify(auditWriter, never()).saveAll(any());
        verify(scheduleRepository, never()).save(any());
        assertThat(schedule.getSlots()).singleElement().satisfies(slot ->
                assertThat(slot.getStartTimeWindow()).isEqualTo(LocalTime.of(9, 0)));
    }
}
