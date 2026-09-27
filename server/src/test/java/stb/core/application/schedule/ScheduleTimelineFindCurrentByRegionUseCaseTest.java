package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.reason.AlterationReasonFormatter;
import stb.core.domain.model.*;
import stb.core.domain.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScheduleTimelineFindCurrentByRegionUseCaseTest {

    @Test
    void returnsReasonsOfCurrentVersionOnly() {
        Region region = new Region("Region", "Office", 0, 0, null);
        ReflectionTestUtils.setField(region, "id", 1);
        Schedule previous = new Schedule(LocalDate.now(), region);
        ReflectionTestUtils.setField(previous, "id", 10);
        previous.markAsCreated();
        Schedule current = Schedule.createFrom(previous);
        ReflectionTestUtils.setField(current, "id", 11);
        EngineerCrew crew = new EngineerCrew("Crew", region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", 20);
        Request affected = Request.builder().build();
        ReflectionTestUtils.setField(affected, "id", 30);
        Request cause = Request.builder().build();
        ReflectionTestUtils.setField(cause, "id", 40);
        AlterationReason reason = AlterationReason.causedByRequest(
                current, affected, AlterationReason.Type.REMOVED_AFTER_REQUEST_CANCELLATION,
                cause, LocalDateTime.now());
        ReflectionTestUtils.setField(reason, "id", 60);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        RegionRepository regions = mock(RegionRepository.class);
        RequestRepository requests = mock(RequestRepository.class);
        EngineerCrewRepository crews = mock(EngineerCrewRepository.class);
        EngineerCrewOffShiftRecordRepository offShiftRecords = mock(EngineerCrewOffShiftRecordRepository.class);
        AlterationReasonRepository reasons = mock(AlterationReasonRepository.class);
        when(regions.findById(1)).thenReturn(Optional.of(region));
        when(schedules.findByTargetDateAndRegionId(LocalDate.now(), 1)).thenReturn(Optional.of(current));
        when(requests.findAllUnassignedByRegionIdAndStatus(1, RequestStatus.Keyword.IDLE)).thenReturn(List.of());
        when(crews.findAllByRegionId(1)).thenReturn(List.of(crew));
        when(offShiftRecords.findAllByEngineerCrewRegionIdAndOffShiftDate(1, LocalDate.now()))
                .thenReturn(List.of(new EngineerCrewOffShiftRecord(crew, LocalDate.now())));
        when(reasons.findAllByScheduleId(11)).thenReturn(List.of(reason));

        var result = new ScheduleTimelineFindCurrentByRegionUseCase(
                schedules, regions, requests, crews, offShiftRecords,
                reasons, new AlterationReasonFormatter()).execute(1);

        assertThat(result.alterationReasons()).singleElement().satisfies(item -> {
            assertThat(item.requestId()).isEqualTo(30);
            assertThat(item.description()).contains("после отмены заявки №40");
        });
        assertThat(result.engineers()).singleElement().satisfies(row -> assertThat(row.offShift()).isTrue());
        verify(reasons).findAllByScheduleId(11);
        verifyNoMoreInteractions(reasons);
    }

    @Test
    void firstScheduleReturnsItsUnscheduledReasons() {
        Region region = new Region("Region", "Office", 0, 0, null);
        ReflectionTestUtils.setField(region, "id", 1);
        Schedule schedule = new Schedule(LocalDate.now(), region);
        ReflectionTestUtils.setField(schedule, "id", 10);
        schedule.markAsCreated();
        Skill skill = new Skill("Монтаж", Skill.Keyword.INSTALLATION_OR_ADDON);
        Request request = Request.builder()
                .fullAddress("Адрес")
                .status(new RequestStatus("Новая", RequestStatus.Keyword.IDLE))
                .deadline(LocalDateTime.now().plusDays(1))
                .jobType(new JobType("Монтаж", List.of(skill), 30, 60, 90, JobType.Keyword.INSTALLATION, 3))
                .region(region)
                .startTimeWindow(LocalTime.of(12, 0))
                .endTimeWindow(LocalTime.of(14, 0))
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(request, "id", 30);
        AlterationReason unscheduledReason = AlterationReason.unscheduled(
                schedule, request, AlterationReason.Type.NO_AVAILABLE_TIME_SLOT, LocalDateTime.now());
        ReflectionTestUtils.setField(unscheduledReason, "id", 40);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        RegionRepository regions = mock(RegionRepository.class);
        RequestRepository requests = mock(RequestRepository.class);
        EngineerCrewRepository crews = mock(EngineerCrewRepository.class);
        EngineerCrewOffShiftRecordRepository offShiftRecords = mock(EngineerCrewOffShiftRecordRepository.class);
        AlterationReasonRepository reasons = mock(AlterationReasonRepository.class);
        when(regions.findById(1)).thenReturn(Optional.of(region));
        when(schedules.findByTargetDateAndRegionId(LocalDate.now(), 1)).thenReturn(Optional.of(schedule));
        when(requests.findAllUnassignedByRegionIdAndStatus(anyInt(), any())).thenReturn(List.of(request));
        when(crews.findAllByRegionId(anyInt())).thenReturn(List.of());
        when(offShiftRecords.findAllByEngineerCrewRegionIdAndOffShiftDate(anyInt(), any())).thenReturn(List.of());
        when(reasons.findAllByScheduleId(10)).thenReturn(List.of(unscheduledReason));

        var result = new ScheduleTimelineFindCurrentByRegionUseCase(
                schedules, regions, requests, crews, offShiftRecords,
                reasons, new AlterationReasonFormatter()).execute(1);

        assertThat(result.unassignedRequests()).extracting(item -> item.id()).containsExactly(30);
        assertThat(result.alterationReasons()).singleElement().satisfies(item -> {
            assertThat(item.requestId()).isEqualTo(30);
            assertThat(item.description()).contains("12:00–14:00");
        });
        verify(reasons).findAllByScheduleId(10);
    }
}
