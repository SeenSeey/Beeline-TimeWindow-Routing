package stb.core.application.schedule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.application.support.reason.SlotAlterationReasonFormatter;
import stb.core.domain.dto.ScheduleTimelineDto;
import stb.core.domain.dto.SlotAlterationReasonDto;
import stb.core.domain.dto.UnassignedRequestDto;
import stb.core.domain.exception.RegionNotFoundException;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.Region;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.model.Schedule;
import stb.core.domain.repository.EngineerCrewOffShiftRecordRepository;
import stb.core.domain.repository.EngineerCrewRepository;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.repository.SlotAlterationReasonRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleTimelineFindCurrentByRegionUseCase {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ScheduleRepository scheduleRepository;
    private final RegionRepository regionRepository;
    private final RequestRepository requestRepository;
    private final EngineerCrewRepository engineerCrewRepository;
    private final EngineerCrewOffShiftRecordRepository engineerCrewOffShiftRecordRepository;
    private final SlotAlterationReasonRepository reasonRepository;
    private final SlotAlterationReasonFormatter reasonFormatter;

    @Transactional(readOnly = true)
    public ScheduleTimelineDto execute(int regionId) {
        LocalDate targetDate = LocalDate.now();

        Region region = regionRepository
            .findById(regionId)
            .orElseThrow(() -> new RegionNotFoundException("Регион не существует"));

        Schedule schedule = scheduleRepository
            .findByTargetDateAndRegionId(targetDate, regionId)
            .orElseThrow(() ->
                new ScheduleNotFoundException(
                    "Расписание на %s для региона %s ещё не сформировано".formatted(
                        dateTimeFormatter.format(targetDate),
                        region.getDisplayName()
                    )
                )
            );

        // every crew of the region is shown, the ones taken off shift today marked as such
        return ScheduleTimelineDto.from(
            schedule,
            findUnassignedRequests(schedule, regionId),
            engineerCrewRepository.findAllByRegionId(regionId),
            findOffShiftCrewIds(regionId, targetDate),
            findAlterationReasons(schedule)
        );
    }

    private Set<Integer> findOffShiftCrewIds(int regionId, LocalDate targetDate) {
        return engineerCrewOffShiftRecordRepository
            .findAllByEngineerCrewRegionIdAndOffShiftDate(regionId, targetDate)
            .stream()
            .map(record -> record.getEngineerCrew().getId())
            .collect(Collectors.toSet());
    }

    private List<SlotAlterationReasonDto> findAlterationReasons(Schedule schedule) {
        Schedule previousSchedule = schedule.getPreviousSchedule();
        if (previousSchedule == null) {
            return List.of();
        }
        return reasonRepository.findAllForCurrentTransition(
                        schedule.getId(), previousSchedule.getId())
                .stream()
                .map(reason -> SlotAlterationReasonDto.from(reason, reasonFormatter.format(reason)))
                .toList();
    }

    private List<UnassignedRequestDto> findUnassignedRequests(Schedule schedule, int regionId) {
        if (schedule.getStatus() != Schedule.Status.CREATED) {
            return List.of();
        }

        return requestRepository
            .findAllUnassignedByRegionIdAndStatus(regionId, RequestStatus.Keyword.IDLE)
            .stream()
            .map(UnassignedRequestDto::from)
            .toList();
    }
}
