package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.domain.model.*;
import stb.core.domain.repository.AlterationReasonRepository;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.service.RouteDomainService;
import stb.core.domain.service.SpeedProfileDomainService;
import stb.core.domain.service.CrewEquipmentDomainService;
import stb.core.domain.service.UnscheduledRequestReasonDomainService;
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
    void separatesAssignmentAuditsFromUnscheduledReasons() {
        RequestRepository requestRepository = mock(RequestRepository.class);
        ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
        EngineerCrewRepository crewRepository = mock(EngineerCrewRepository.class);
        RequestStatusRepository statusRepository = mock(RequestStatusRepository.class);
        ScheduleAssemblerDomainService assembler = mock(ScheduleAssemblerDomainService.class);
        RouteDomainService routeService = mock(RouteDomainService.class);
        SpeedProfileDomainService speedProfileService = mock(SpeedProfileDomainService.class);
        RequestActionAuditWriter auditWriter = mock(RequestActionAuditWriter.class);
        AlterationReasonRepository reasonRepository = mock(AlterationReasonRepository.class);

        Region region = new Region("Регион", "Офис", 55.7, 37.6, null);
        Skill skill = new Skill("Монтаж", Skill.Keyword.INSTALLATION_OR_ADDON);
        Vehicle vehicle = new Vehicle("Автомобиль", Vehicle.Keyword.CAR, 40, 2);
        EngineerCrew crew = new EngineerCrew(
                "Бригада 1", region, vehicle, LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(skill), List.of());
        ReflectionTestUtils.setField(crew, "id", 10);
        JobType jobType = new JobType("Монтаж", List.of(skill), 30, 60, 90, JobType.Keyword.INSTALLATION, 3);
        Request request = Request.builder()
                .fullAddress("Адрес")
                .latitude(55.8)
                .longitude(37.7)
                .region(region)
                .jobType(jobType)
                .startTimeWindow(LocalTime.of(9, 0))
                .endTimeWindow(LocalTime.NOON)
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(request, "id", 1);
        Request unassigned = Request.builder()
                .fullAddress("Другой адрес")
                .latitude(55.9)
                .longitude(37.8)
                .region(region)
                .jobType(jobType)
                .startTimeWindow(LocalTime.of(12, 0))
                .endTimeWindow(LocalTime.of(14, 0))
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(unassigned, "id", 2);
        Schedule schedule = new Schedule(LocalDate.of(2026, 9, 25), region);
        RequestStatus assigned = new RequestStatus("Назначена", RequestStatus.Keyword.ASSIGNED);
        LocalTime start = LocalTime.of(10, 30);

        when(scheduleRepository.findAllByTargetDateAndStatus(any(), eq(Schedule.Status.CALCULATING)))
                .thenReturn(List.of(schedule));
        when(statusRepository.findAll()).thenReturn(List.of(assigned));
        when(requestRepository.findAllForSchedulingByRegionId(0)).thenReturn(List.of(request, unassigned));
        when(crewRepository.findAllActiveByRegionIdAndTargetDate(0, LocalDate.of(2026, 9, 25))).thenReturn(List.of(crew));
        when(routeService.calculateAllRoutes(anyList(), any())).thenReturn(List.of());
        when(speedProfileService.createSpeedProfile()).thenReturn(mock(SpeedProfile.class));
        when(assembler.createSchedule(any())).thenReturn(new ScheduleCalculationResult(List.of(
                new OutputSlotDto(1, 10, 60, LocalTime.of(9, 0), LocalTime.NOON,
                        LocalTime.of(10, 20), start, LocalTime.of(11, 30), null),
                new OutputSlotDto(2, null, 60, LocalTime.NOON, LocalTime.of(14, 0),
                        null, null, null, null)
        )));

        ScheduleCalculateUseCase useCase = new ScheduleCalculateUseCase(
                requestRepository, scheduleRepository, crewRepository, statusRepository,
                assembler, routeService, speedProfileService, mock(CrewEquipmentDomainService.class), auditWriter,
                new UnscheduledRequestReasonDomainService(), reasonRepository);
        useCase.execute(LocalDate.of(2026, 9, 25));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestActionAudit>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditWriter).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(audit -> {
            assertThat(audit.getRequest()).isSameAs(request);
            assertThat(audit.getAction()).isEqualTo(RequestActionAudit.Action.ASSIGNED);
            assertThat(audit.getAssignedCrew()).isSameAs(crew);
            assertThat(audit.getPlannedStart()).isEqualTo(start);
            assertThat(audit.getPlannedEnd()).isEqualTo(LocalTime.of(11, 30));
        });
        assertThat(request.getAssignedCrew()).isSameAs(crew);
        assertThat(schedule.getSlots()).hasSize(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlterationReason>> reasonCaptor = ArgumentCaptor.forClass(List.class);
        verify(reasonRepository).saveAll(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).singleElement().satisfies(reason -> {
            assertThat(reason.getSchedule()).isSameAs(schedule);
            assertThat(reason.getRequest()).isSameAs(unassigned);
            assertThat(reason.getType()).isEqualTo(AlterationReason.Type.NO_AVAILABLE_TIME_SLOT);
        });
    }
}
