package stb.core.infrastructure.event.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import stb.core.application.schedule.ScheduleCalculateUseCase;
import stb.core.application.schedule.ScheduleCalculatingDeleteUseCase;
import stb.core.domain.event.SchedulesCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleCalculationListener {

    private final ScheduleCalculateUseCase scheduleCalculateUseCase;
    private final ScheduleCalculatingDeleteUseCase scheduleCalculatingDeleteUseCase;

    @Async("scheduleExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SchedulesCreatedEvent event) {
        log.info("Расчёт расписаний для {}", event.targetDate());

        try {
            scheduleCalculateUseCase.execute(event.targetDate());
            log.info("Расписания для {} успешно рассчитаны", event.targetDate());
        } catch (Exception exception) {
            log.error("Ошибка расчёта расписаний для {}", event.targetDate(), exception);
            scheduleCalculatingDeleteUseCase.execute();
        }
    }
}