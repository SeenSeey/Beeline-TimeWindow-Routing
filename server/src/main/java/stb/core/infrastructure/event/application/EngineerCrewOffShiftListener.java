package stb.core.infrastructure.event.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import stb.core.application.schedule.ScheduleRecalculateOnCrewOfShiftUseCase;
import stb.core.domain.event.EngineerCrewTakenOffShiftEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class EngineerCrewOffShiftListener {

    private final ScheduleRecalculateOnCrewOfShiftUseCase scheduleRecalculateOnCrewOfShiftUseCase;

    @Async("scheduleExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EngineerCrewTakenOffShiftEvent event) {
        log.info("Инженер {} снят со смены, попытка пересчёта расписания", event.engineerCrewId());

        try {
            scheduleRecalculateOnCrewOfShiftUseCase.execute(event.engineerCrewId());
            log.info("Обработка завершения");
        } catch (Exception exception) {
            log.info("Ошибка при обработке события", exception);
        }
    }

}
