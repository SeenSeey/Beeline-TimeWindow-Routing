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
import stb.core.domain.dto.ScheduleTimelineDto;
import stb.core.domain.dto.UnassignedRequestDto;
import stb.core.domain.exception.RegionNotFoundException;
import stb.core.domain.exception.ScheduleNotFoundException;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.Schedule;
import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.ScheduleRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleTimelineFindCurrentByRegionUseCase {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ScheduleRepository scheduleRepository;
    private final RegionRepository regionRepository;
    private final RequestRepository requestRepository;

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

        return ScheduleTimelineDto.from(schedule, findUnassignedRequests(schedule, regionId));
    }

    private List<UnassignedRequestDto> findUnassignedRequests(Schedule schedule, int regionId) {
        if (schedule.getStatus() != Schedule.Status.CREATED) {
            return List.of();
        }

        Set<Integer> scheduledRequestIds = schedule
            .getSlots()
            .stream()
            .map(ScheduleSlot::getRequest)
            .filter(request -> request.getAssignedCrew() != null)
            .map(Request::getId)
            .collect(Collectors.toSet());

        return requestRepository
            .findAllActiveByRegionId(regionId)
            .stream()
            .filter(request -> !scheduledRequestIds.contains(request.getId()))
            .map(UnassignedRequestDto::from)
            .toList();
    }
}
