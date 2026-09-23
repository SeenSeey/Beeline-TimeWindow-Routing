package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.dto.ScheduleTimelineDto;
import stb.core.domain.event.SchedulesCreatedEvent;
import stb.core.domain.exception.ScheduleAlreadyExistsException;
import stb.core.domain.model.*;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleInitialBuildUseCase {

    private final RegionRepository regionRepository;
    private final ScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public List<ScheduleTimelineDto> execute() {
        LocalDate targetDate = LocalDate.now();

        if (scheduleRepository.countAllByTargetDate(targetDate) > 0) {
            throw new ScheduleAlreadyExistsException("Расписание на %s уже сформировано".formatted(targetDate));
        }

        List<Region> regions = regionRepository.findAll();
        List<Schedule> schedules = regions.stream()
                .map(region -> new Schedule(targetDate, region))
                .toList();

        scheduleRepository.saveAll(schedules);

        applicationEventPublisher.publishEvent(new SchedulesCreatedEvent(targetDate));

        return schedules.stream().map(ScheduleTimelineDto::from).toList();
    }
}
