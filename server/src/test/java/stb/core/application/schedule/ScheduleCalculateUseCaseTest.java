package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import stb.core.application.audit.RequestActionAuditService;
import stb.core.domain.model.EngineerCrew;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.model.Schedule;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.service.RouteDomainService;
import stb.core.domain.service.SpeedProfileDomainService;
import stb.core.domain.service.replanning.RequestActionAuditFormatter;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleCalculateUseCaseTest {

    @Test
    void auditsEveryActualInitialAssignmentWithOneOperationId() {
        RequestRepository requestRepository = mock(RequestRepository.class);
        ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
        EngineerCrewRepository crewRepository = mock(EngineerCrewRepository.class);
        RequestStatusRepository statusRepository = mock(RequestStatusRepository.class);
        ScheduleAssemblerDomainService assembler = mock(ScheduleAssemblerDomainService.class);
        RouteDomainService routeService = mock(RouteDomainService.class);
        SpeedProfileDomainService speedProfileService = mock(SpeedProfileDomainService.class);
        RequestActionAuditService auditService = mock(RequestActionAuditService.class);

        Region region = new Region("Регион", "Офис", 55.7, 37.6, null);
        EngineerCrew crew = new EngineerCrew(
                "Бригада 1", region, null, LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        Request request = Request.builder()
                .fullAddress("Адрес")
                .latitude(55.8)
                .longitude(37.7)
                .region(region)
                .createdAt(LocalDateTime.now())
                .build();
        Schedule schedule = new Schedule(LocalDate.of(2026, 9, 25), region);
        RequestStatus assigned = new RequestStatus("Назначена", RequestStatus.Keyword.ASSIGNED);
        LocalTime start = LocalTime.of(10, 30);

        when(scheduleRepository.findAllByTargetDateAndStatus(any(), eq(Schedule.Status.CALCULATING)))
                .thenReturn(List.of(schedule));
        when(statusRepository.findAll()).thenReturn(List.of(assigned));
        when(requestRepository.findAllForSchedulingByRegionId(0)).thenReturn(List.of(request));
        when(crewRepository.findAllByRegionId(0)).thenReturn(List.of(crew));
        when(routeService.calculateAllRoutes(anyList(), any())).thenReturn(List.of());
        when(speedProfileService.createSpeedProfile()).thenReturn(mock(SpeedProfile.class));
        when(assembler.createSchedule(any())).thenReturn(new ScheduleCalculationResult(List.of(
                new OutputSlotDto(0, 0, 60, LocalTime.of(9, 0), LocalTime.NOON,
                        LocalTime.of(10, 20), start, LocalTime.of(11, 30), null)
        )));

        ScheduleCalculateUseCase useCase = new ScheduleCalculateUseCase(
                requestRepository, scheduleRepository, crewRepository, statusRepository,
                assembler, routeService, speedProfileService, new RequestActionAuditFormatter(), auditService);
        useCase.execute(LocalDate.of(2026, 9, 25));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAuditService.Entry>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditService).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(entry -> {
            assertThat(entry.request()).isSameAs(request);
            assertThat(entry.action()).isEqualTo(RequestActionAudit.Action.INITIAL_ASSIGNMENT);
            assertThat(entry.assignedCrew()).isSameAs(crew);
            assertThat(entry.schedule()).isSameAs(schedule);
            assertThat(entry.operationId()).isNotNull();
            assertThat(entry.description()).contains("Бригада 1", "10:30");
        });
        assertThat(request.getAssignedCrew()).isSameAs(crew);
        assertThat(schedule.getSlots()).hasSize(1);
    }
}
