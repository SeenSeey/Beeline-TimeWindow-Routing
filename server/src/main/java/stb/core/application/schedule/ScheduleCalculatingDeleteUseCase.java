package stb.core.application.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stb.core.domain.model.Schedule;
import stb.core.domain.repository.ScheduleRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleCalculatingDeleteUseCase {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void execute() {
        long deletedCount = scheduleRepository.deleteAllByStatus(Schedule.Status.CALCULATING);

        if (deletedCount > 0) {
            log.warn("Удалено {} нерассчитанных расписаний", deletedCount);
        }
    }
}
