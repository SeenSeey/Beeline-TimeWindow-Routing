package stb.core.infrastructure.event.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import stb.core.application.schedule.ScheduleEmergencyHandleUseCase;
import stb.core.domain.event.EmergencyCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencyCreatedListener {

    private final ScheduleEmergencyHandleUseCase scheduleEmergencyHandleUseCase;

    @Async("scheduleExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EmergencyCreatedEvent event) {
        log.info("Зарегистрирована срочная заявка {} для региона {}, переформирование расписания", event.requestId(), event.regionId());

        try {
            scheduleEmergencyHandleUseCase.execute(event.requestId());
            log.info("Обработка завершения");
        } catch (Exception exception) {
            log.error("Ошибка при переформировании расписания", exception);
        }
    }
}
