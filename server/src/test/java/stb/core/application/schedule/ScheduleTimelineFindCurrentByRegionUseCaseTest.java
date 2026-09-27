package stb.core.application.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.application.support.reason.SlotAlterationReasonFormatter;
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
    void returnsRemovedReasonFromPreviousScheduleAsTopLevelItem() {
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
        ScheduleSlot removedSlot = new ScheduleSlot(previous, affected, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(10, 0));
        ReflectionTestUtils.setField(removedSlot, "id", 50);
        SlotAlterationReason reason = SlotAlterationReason.causedByRequest(
                removedSlot, SlotAlterationReason.Action.REMOVED_AFTER_REQUEST_CANCELLATION,
                cause, LocalDateTime.now());
        ReflectionTestUtils.setField(reason, "id", 60);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        RegionRepository regions = mock(RegionRepository.class);
        RequestRepository requests = mock(RequestRepository.class);
        EngineerCrewRepository crews = mock(EngineerCrewRepository.class);
        EngineerCrewOffShiftRecordRepository offShiftRecords = mock(EngineerCrewOffShiftRecordRepository.class);
        SlotAlterationReasonRepository reasons = mock(SlotAlterationReasonRepository.class);
        when(regions.findById(1)).thenReturn(Optional.of(region));
        when(schedules.findByTargetDateAndRegionId(LocalDate.now(), 1)).thenReturn(Optional.of(current));
        when(requests.findAllUnassignedByRegionIdAndStatus(1, RequestStatus.Keyword.IDLE)).thenReturn(List.of());
        when(crews.findAllByRegionId(1)).thenReturn(List.of(crew));
        when(offShiftRecords.findAllByEngineerCrewRegionIdAndOffShiftDate(1, LocalDate.now()))
                .thenReturn(List.of(new EngineerCrewOffShiftRecord(crew, LocalDate.now())));
        when(reasons.findAllForCurrentTransition(11, 10))
                .thenReturn(List.of(reason));

        var result = new ScheduleTimelineFindCurrentByRegionUseCase(
                schedules, regions, requests, crews, offShiftRecords,
                reasons, new SlotAlterationReasonFormatter()).execute(1);

        assertThat(result.alterationReasons()).singleElement().satisfies(item -> {
            assertThat(item.action()).isEqualTo(SlotAlterationReason.Action.REMOVED_AFTER_REQUEST_CANCELLATION);
            assertThat(item.affectedRequestId()).isEqualTo(30);
            assertThat(item.slotScheduleId()).isEqualTo(10);
            assertThat(item.causeRequestId()).isEqualTo(40);
        });
        assertThat(result.engineers()).singleElement().satisfies(row -> assertThat(row.offShift()).isTrue());
        verify(reasons).findAllForCurrentTransition(11, 10);
    }

    @Test
    void firstScheduleHasNoReplanningReasons() {
        Region region = new Region("Region", "Office", 0, 0, null);
        ReflectionTestUtils.setField(region, "id", 1);
        Schedule schedule = new Schedule(LocalDate.now(), region);
        schedule.markAsCreated();
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        RegionRepository regions = mock(RegionRepository.class);
        RequestRepository requests = mock(RequestRepository.class);
        EngineerCrewRepository crews = mock(EngineerCrewRepository.class);
        EngineerCrewOffShiftRecordRepository offShiftRecords = mock(EngineerCrewOffShiftRecordRepository.class);
        SlotAlterationReasonRepository reasons = mock(SlotAlterationReasonRepository.class);
        when(regions.findById(1)).thenReturn(Optional.of(region));
        when(schedules.findByTargetDateAndRegionId(LocalDate.now(), 1)).thenReturn(Optional.of(schedule));
        when(requests.findAllUnassignedByRegionIdAndStatus(anyInt(), any())).thenReturn(List.of());
        when(crews.findAllByRegionId(anyInt())).thenReturn(List.of());
        when(offShiftRecords.findAllByEngineerCrewRegionIdAndOffShiftDate(anyInt(), any())).thenReturn(List.of());

        var result = new ScheduleTimelineFindCurrentByRegionUseCase(
                schedules, regions, requests, crews, offShiftRecords,
                reasons, new SlotAlterationReasonFormatter()).execute(1);

        assertThat(result.alterationReasons()).isEmpty();
        verifyNoInteractions(reasons);
    }
}
