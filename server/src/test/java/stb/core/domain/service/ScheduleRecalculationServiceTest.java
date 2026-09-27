package stb.core.domain.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import stb.core.domain.model.*;
import stb.core.domain.replanning.ScheduleVersionDiff;
import stb.core.domain.replanning.ScheduleVersionDiffDomainService;
import stb.core.domain.repository.*;
import stb.core.domain.service.scheduler.ScheduleAssemblerDomainService;
import stb.core.domain.service.scheduler.dto.ScheduleCalculationResult;
import stb.core.domain.service.scheduler.dto.output.OutputSlotDto;
import stb.core.domain.service.scheduler.impl.logic.speed.SpeedProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScheduleRecalculationServiceTest {

    private final Region region = region();
    private final RequestRepository requests = mock(RequestRepository.class);
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final EngineerCrewRepository crews = mock(EngineerCrewRepository.class);
    private final ScheduleSlotRepository slots = mock(ScheduleSlotRepository.class);
    private final RequestStatusRepository statuses = mock(RequestStatusRepository.class);
    private final ScheduleAssemblerDomainService assembler = mock(ScheduleAssemblerDomainService.class);
    private final SpeedProfileDomainService speeds = mock(SpeedProfileDomainService.class);
    private final RouteDomainService routes = mock(RouteDomainService.class);
    private final ScheduleRecalculationService service = new ScheduleRecalculationService(
            requests, schedules, crews, slots, statuses, assembler, speeds, routes,
            new ScheduleVersionDiffDomainService());

    @Test
    void removesCanceledRequestFromNewVersionAndUnassignsIt() {
        EngineerCrew crew = crew(2);
        RequestStatus canceledStatus = new RequestStatus("Canceled", RequestStatus.Keyword.CANCELED);
        Request canceled = Request.builder()
                .region(region)
                .status(canceledStatus)
                .assignedCrew(crew)
                .assignedAt(LocalDateTime.now().minusHours(1))
                .canceledAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(canceled, "id", 3);
        Schedule previous = previousSchedule(canceled, crew);
        ScheduleSlot removedSlot = previous.getSlots().getFirst();
        givenRecalculation(previous, List.of(), List.of(crew), List.of(canceledStatus), List.of());

        ScheduleVersionDiff diff = service.recalculateSchedule(region, LocalDate.now());

        Schedule current = savedSchedule();
        assertThat(current.getPreviousSchedule()).isSameAs(previous);
        assertThat(current.getSlots()).isEmpty();
        assertThat(previous.getSlots()).containsExactly(removedSlot);
        assertThat(diff.removed()).containsExactly(removedSlot);
        assertThat(canceled.getAssignedCrew()).isNull();
        assertThat(canceled.getStatus()).isSameAs(canceledStatus);
    }

    @Test
    void movesRequestToActiveCrewWithCrewSnapshotInNewVersion() {
        EngineerCrew unavailableCrew = crew(2);
        EngineerCrew activeCrew = crew(3);
        RequestStatus assignedStatus = new RequestStatus("Assigned", RequestStatus.Keyword.ASSIGNED);
        Request request = Request.builder()
                .region(region)
                .status(assignedStatus)
                .assignedCrew(unavailableCrew)
                .assignedAt(LocalDateTime.now().minusHours(1))
                .build();
        ReflectionTestUtils.setField(request, "id", 4);
        Schedule previous = previousSchedule(request, unavailableCrew);
        givenRecalculation(previous, List.of(request), List.of(activeCrew), List.of(assignedStatus), List.of(
                new OutputSlotDto(4, 3, 60,
                        LocalTime.of(8, 0), LocalTime.of(18, 0),
                        LocalTime.of(9, 50), LocalTime.of(10, 0), LocalTime.of(11, 0), null)));

        ScheduleVersionDiff diff = service.recalculateSchedule(region, LocalDate.now());

        Schedule current = savedSchedule();
        assertThat(current.getSlots()).singleElement().satisfies(slot ->
                assertThat(slot.getEngineerCrew()).isSameAs(activeCrew));
        assertThat(previous.getSlots().getFirst().getEngineerCrew()).isSameAs(unavailableCrew);
        assertThat(diff.moved()).singleElement().satisfies(change ->
                assertThat(change.currentSlot().getSchedule()).isSameAs(current));
        assertThat(request.getAssignedCrew()).isSameAs(activeCrew);
    }

    private void givenRecalculation(Schedule previous,
                                    List<Request> requestsToReschedule,
                                    List<EngineerCrew> activeCrews,
                                    List<RequestStatus> statusValues,
                                    List<OutputSlotDto> output) {
        when(schedules.findByTargetDateAndRegionId(LocalDate.now(), 1)).thenReturn(Optional.of(previous));
        when(requests.findAllForRescheduleByRegionId(1)).thenReturn(requestsToReschedule);
        when(crews.findAllActiveByRegionIdAndTargetDate(1, LocalDate.now())).thenReturn(activeCrews);
        when(slots.findLatestSlotsForCrewsByRegionId(1)).thenReturn(List.of());
        when(statuses.findAll()).thenReturn(statusValues);
        when(routes.calculateAllRoutes(anyList(), any())).thenReturn(List.of());
        when(speeds.createSpeedProfile()).thenReturn(mock(SpeedProfile.class));
        when(assembler.createSchedule(any())).thenReturn(new ScheduleCalculationResult(output));
    }

    private Schedule savedSchedule() {
        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(schedules).save(captor.capture());
        return captor.getValue();
    }

    private Schedule previousSchedule(Request request, EngineerCrew crew) {
        Schedule previous = new Schedule(LocalDate.now(), region);
        ReflectionTestUtils.setField(previous, "id", 5);
        previous.markAsCreated();
        previous.addSlot(request, crew, ScheduleSlot.SlotType.REQUEST,
                LocalTime.of(8, 50), LocalTime.of(9, 0), LocalTime.of(10, 0));
        return previous;
    }

    private EngineerCrew crew(int id) {
        EngineerCrew crew = new EngineerCrew("Crew " + id, region, null,
                LocalTime.of(8, 0), LocalTime.of(18, 0), List.of(), List.of());
        ReflectionTestUtils.setField(crew, "id", id);
        return crew;
    }

    private static Region region() {
        Region region = new Region("Region", "Office", 55.7, 37.6, null);
        ReflectionTestUtils.setField(region, "id", 1);
        return region;
    }
}
